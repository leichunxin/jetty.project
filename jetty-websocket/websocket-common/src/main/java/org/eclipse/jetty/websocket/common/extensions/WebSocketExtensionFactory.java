//
//  ========================================================================
//  Copyright (c) 1995-2015 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.websocket.common.extensions;

import java.util.Objects;

import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.util.DecoratedObjectFactory;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.websocket.api.WebSocketException;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.api.extensions.Extension;
import org.eclipse.jetty.websocket.api.extensions.ExtensionConfig;
import org.eclipse.jetty.websocket.api.extensions.ExtensionFactory;
import org.eclipse.jetty.websocket.common.scopes.WebSocketContainerScope;

public class WebSocketExtensionFactory extends ExtensionFactory
{
    private final DecoratedObjectFactory objFactory;
    private final WebSocketPolicy policy;
    private final ByteBufferPool bufferPool;

    public WebSocketExtensionFactory(WebSocketContainerScope container)
    {
        this(container.getPolicy(), container.getObjectFactory(), container.getBufferPool());
    }

    public WebSocketExtensionFactory(WebSocketPolicy policy, DecoratedObjectFactory objectFactory, ByteBufferPool byteBufferPool)
    {
        super();
        Objects.requireNonNull(objectFactory,"DecoratedObjectFactory");
        this.policy = policy;
        this.objFactory = objectFactory;
        this.bufferPool = byteBufferPool;
    }

    @Override
    public Extension newInstance(ExtensionConfig config)
    {
        if (config == null)
        {
            return null;
        }

        String name = config.getName();
        if (StringUtil.isBlank(name))
        {
            return null;
        }

        Class<? extends Extension> extClass = getExtension(name);
        if (extClass == null)
        {
            return null;
        }

        try
        {
            Extension ext = objFactory.createInstance(extClass);
            if (ext instanceof AbstractExtension)
            {
                AbstractExtension aext = (AbstractExtension)ext;
                aext.init(policy, bufferPool);
                aext.setConfig(config);
            }
            return ext;
        }
        catch (InstantiationException | IllegalAccessException e)
        {
            throw new WebSocketException("Cannot instantiate extension: " + extClass,e);
        }
    }
}
