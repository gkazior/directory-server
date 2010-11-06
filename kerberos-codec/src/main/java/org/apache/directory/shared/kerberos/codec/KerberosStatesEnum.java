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
package org.apache.directory.shared.kerberos.codec;


import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.States;


/**
 * This class store the Kerberos grammar's constants. It is also used for debugging
 * purpose
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum KerberosStatesEnum implements States
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------

    /** The END_STATE */
    END_STATE,

    // Start
    START_STATE,
    
    // ----- Ticket message --------------------------------------------
    TICKET_STATE,
    TICKET_SEQ_STATE,
    
    TICKET_VNO_TAG_STATE,
    TICKET_VNO_STATE,
    
    TICKET_REALM_TAG_STATE,
    TICKET_REALM_STATE,
    
    TICKET_SNAME_TAG_STATE,

    TICKET_ENC_PART_TAG_STATE,
    
    // ----- PrincipalName message --------------------------------------
    PRINCIPAL_NAME_STATE,
    
    PRINCIPAL_NAME_NAME_TYPE_TAG_STATE,
    PRINCIPAL_NAME_NAME_TYPE_STATE,
    
    PRINCIPAL_NAME_NAME_STRING_SEQ_STATE,
    
    PRINCIPAL_NAME_NAME_STRING_TAG_STATE,
    PRINCIPAL_NAME_NAME_STRING_STATE,
    
    // End
    LAST_KERBEROS_STATE;

    
    /**
     * Get the grammar name
     * 
     * @param grammar The grammar code
     * @return The grammar name
     */
    public String getGrammarName( int grammar )
    {
        return "KERBEROS_MESSAGE_GRAMMAR";
    }


    /**
     * Get the grammar name
     * 
     * @param grammar The grammar class
     * @return The grammar name
     */
    public String getGrammarName( Grammar grammar )
    {
        if ( grammar instanceof KerberosMessageGrammar )
        {
            return "KERBEROS_MESSAGE_GRAMMAR";
        }
        else
        {
            return "UNKNOWN GRAMMAR";
        }
    }


    /**
     * Get the string representing the state
     * 
     * @param state The state number
     * @return The String representing the state
     */
    public String getState( int state )
    {
        return ( ( state == END_STATE.ordinal() ) ? "KERBEROS_MESSAGE_END_STATE" : name() );
    }

    
    /**
     * {@inheritDoc}
     */
    public boolean isEndState()
    {
        return this == END_STATE;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public KerberosStatesEnum getStartState()
    {
        return START_STATE;
    }
}
