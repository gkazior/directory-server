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
package org.apache.directory.server.kerberos.kdc.ticketgrant;


import java.net.InetAddress;

import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;
import org.apache.directory.server.kerberos.shared.messages.ApplicationRequest;
import org.apache.directory.server.kerberos.shared.messages.components.Authenticator;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.replay.ReplayCache;
import org.apache.directory.server.kerberos.shared.service.VerifyAuthHeader;
import org.apache.mina.common.IoSession;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class VerifyTgtAuthHeader extends VerifyAuthHeader
{
    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        TicketGrantingContext tgsContext = ( TicketGrantingContext ) session.getAttribute( getContextKey() );

        ApplicationRequest authHeader = tgsContext.getAuthHeader();
        Ticket tgt = tgsContext.getTgt();

        EncryptionType encryptionType = tgt.getEncPart().getEncryptionType();
        EncryptionKey serverKey = tgsContext.getTicketPrincipalEntry().getKeyMap().get( encryptionType );

        long clockSkew = tgsContext.getConfig().getClockSkew();
        ReplayCache replayCache = tgsContext.getReplayCache();
        boolean emptyAddressesAllowed = tgsContext.getConfig().isEmptyAddressesAllowed();
        InetAddress clientAddress = tgsContext.getClientAddress();
        CipherTextHandler cipherTextHandler = tgsContext.getCipherTextHandler();

        Authenticator authenticator = verifyAuthHeader( authHeader, tgt, serverKey, clockSkew, replayCache,
            emptyAddressesAllowed, clientAddress, cipherTextHandler, KeyUsage.NUMBER7 );

        tgsContext.setAuthenticator( authenticator );

        next.execute( session, message );
    }
}
