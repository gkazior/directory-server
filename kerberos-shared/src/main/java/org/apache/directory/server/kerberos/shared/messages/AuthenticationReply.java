/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.kerberos.shared.messages;


import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.shared.kerberos.KerberosMessageType;
import org.apache.directory.shared.kerberos.components.EncryptedData;
import org.apache.directory.shared.kerberos.components.PaData;
import org.apache.directory.shared.kerberos.messages.Ticket;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AuthenticationReply extends KdcReply
{
    /**
     * Creates a new instance of AuthenticationReply.
     */
    public AuthenticationReply()
    {
        super( KerberosMessageType.AS_REP );
    }


    /**
     * Creates a new instance of AuthenticationReply.
     *
     * @param paData
     * @param clientPrincipal
     * @param ticket
     * @param encPart
     */
    public AuthenticationReply( PaData[] paData, KerberosPrincipal clientPrincipal, Ticket ticket,
        EncryptedData encPart )
    {
        super( paData, clientPrincipal, ticket, encPart, KerberosMessageType.AS_REP );
    }
}