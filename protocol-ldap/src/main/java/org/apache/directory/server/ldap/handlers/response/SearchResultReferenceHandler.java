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
package org.apache.directory.server.ldap.handlers.response;


import org.apache.directory.api.ldap.model.message.SearchResultReference;
import org.apache.directory.server.core.api.SearchRequestContainer;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.server.ldap.handlers.LdapResponseHandler;


/**
 * A handler for processing SearchResultReference responses.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SearchResultReferenceHandler extends LdapResponseHandler<SearchResultReference>
{
    /**
     * {@inheritDoc}
     */
    public void handle( LdapSession session, SearchResultReference searchResultReference ) throws Exception
    {
        LOG.debug( "Message Sent : {}", searchResultReference );
        SearchRequestContainer searchRequestContainer = session.getSearchRequest( searchResultReference.getMessageId() );
        
        if ( searchRequestContainer != null )
        {
            searchRequestContainer.increment();
        }
        
        //System.out.println( "SearchResultReference : " + searchRequestContainer );
    }
}
