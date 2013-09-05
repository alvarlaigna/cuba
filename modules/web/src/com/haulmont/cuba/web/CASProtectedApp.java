/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.web;

import com.haulmont.cuba.security.global.LoginException;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

/**
 * @author gorodnov
 * @version $Id$
 */
public class CASProtectedApp extends DefaultApp implements ConnectionListener {

    private static final long serialVersionUID = -6926944868742949956L;

    @Override
    protected Connection createConnection() {
        Connection connection = new CASProtectedConnection();
        connection.addListener(this);
        return connection;
    }

    @Override
    protected boolean loginOnStart() {
        try {
            Principal principal = getPrincipal();
            if (principal != null && principal.getName() != null && !connection.isConnected()) {
                connection.login(principal.getName(), null, locale);

                return true;
            }
        } catch (LoginException e) {
            //do nothing
        }
        return false;
    }
}