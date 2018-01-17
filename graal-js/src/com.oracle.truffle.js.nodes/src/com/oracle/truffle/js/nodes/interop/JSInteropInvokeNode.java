/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.interop;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSFunction;

public abstract class JSInteropInvokeNode extends JavaScriptBaseNode {
    final JSContext context;
    @Child private JSFunctionCallNode callNode;

    JSInteropInvokeNode(JSContext context) {
        this.context = context;
        this.callNode = JSFunctionCallNode.createCall();
    }

    public static JSInteropInvokeNode create(JSContext context) {
        return JSInteropInvokeNodeGen.create(context);
    }

    public abstract Object execute(DynamicObject receiver, String name, Object[] arguments);

    @SuppressWarnings("unused")
    @Specialization(guards = {"cachedName.equals(name)"}, limit = "1")
    Object doCached(DynamicObject receiver, String name, Object[] arguments,
                    @Cached("name") String cachedName,
                    @Cached("createGetProperty(cachedName)") PropertyGetNode functionPropertyGetNode) {
        Object function = functionPropertyGetNode.getValue(receiver);
        if (JSFunction.isJSFunction(function)) {
            return callNode.executeCall(JSArguments.create(receiver, function, arguments));
        } else {
            throw UnknownIdentifierException.raise(cachedName);
        }
    }

    @Specialization(replaces = "doCached")
    Object doUncached(DynamicObject receiver, String name, Object[] arguments,
                    @Cached("create(context)") ReadElementNode readNode) {
        Object function = readNode.executeWithTargetAndIndex(receiver, name);
        if (JSFunction.isJSFunction(function)) {
            return callNode.executeCall(JSArguments.create(receiver, function, arguments));
        } else {
            throw UnknownIdentifierException.raise(name);
        }
    }

    PropertyGetNode createGetProperty(String name) {
        return PropertyGetNode.create(name, false, context);
    }
}
