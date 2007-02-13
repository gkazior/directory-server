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

package org.apache.directory.server.core.subtree;


import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.unit.AbstractAdminTestCase;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;


/**
 * Testcases for the SubentryService. Investigation on handling Subtree Refinement
 * Selection Membership upon entry modifications. As we allow any LDAP filter to be
 * specified as specificationFilter in subtreeSpecifications, any modification on
 * entries can cause changes on subentry operational attributes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SubentryServiceEntryModificationHandlingITest extends AbstractAdminTestCase
{
    public Attributes getTestEntry( String cn )
    {
        Attributes entry = new AttributesImpl();
        Attribute objectClass = new AttributeImpl( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "person" );
        entry.put( objectClass );
        entry.put( "cn", cn );
        entry.put( "sn", cn );
        return entry;
    }

    public Attributes getCollectiveAttributeTestSubentryWithLDAPFilter( String cn, String sn )
    {
        Attributes subentry = new AttributesImpl();
        Attribute objectClass = new AttributeImpl( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "subentry" );
        objectClass.add( "collectiveAttributeSubentry" );
        subentry.put( objectClass );
        subentry.put( "subtreeSpecification", "{ specificationFilter (sn=" + sn + ") }" );
        subentry.put( "c-o", "Test Org" );
        subentry.put( "cn", cn );
        return subentry;
    }


    public void addAdministrativeRoles() throws NamingException
    {
        Attribute attribute = new AttributeImpl( "administrativeRole" );
        attribute.add( "autonomousArea" );
        attribute.add( "collectiveAttributeSpecificArea" );
        ModificationItemImpl item = new ModificationItemImpl( DirContext.ADD_ATTRIBUTE, attribute );
        super.sysRoot.modifyAttributes( "", new ModificationItemImpl[]
            { item } );
    }


    public Map<String, Attributes> getAllEntries() throws NamingException
    {
        Map<String, Attributes> resultMap = new HashMap<String, Attributes>();
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setReturningAttributes( new String[]
            { "+", "*" } );
        NamingEnumeration results = super.sysRoot.search( "", "(objectClass=*)", controls );
        while ( results.hasMore() )
        {
            SearchResult result = ( SearchResult ) results.next();
            resultMap.put( result.getName(), result.getAttributes() );
        }
        return resultMap;
    }
    

    public void testTrackingOfEntryModificationsInSubentryServiceModifyRoutine() throws Exception
    {
        addAdministrativeRoles();
        super.sysRoot.createSubcontext( "cn=collectiveAttributeTestSubentry",
            getCollectiveAttributeTestSubentryWithLDAPFilter( "collectiveAttributeTestSubentry", "testEntry" ) );
        super.sysRoot.createSubcontext( "cn=testEntry", getTestEntry( "testEntry" ) );

        //----------------------------------------------------------------------

        Map results = getAllEntries();
        Attributes testEntry = ( Attributes ) results.get( "cn=testEntry,ou=system" );

        Attribute collectiveAttributeSubentries = testEntry.get( "collectiveAttributeSubentries" );

        assertNotNull( collectiveAttributeSubentries );

        //----------------------------------------------------------------------

        AttributeImpl attr = new AttributeImpl( "sn", "changedSn");
        ModificationItemImpl mod = new ModificationItemImpl(DirContext.REPLACE_ATTRIBUTE, attr);
        ModificationItemImpl[] mods = new ModificationItemImpl[] { mod };
        
        super.sysRoot.modifyAttributes( "cn=testEntry", mods );

        results = getAllEntries();
        testEntry = ( Attributes ) results.get( "cn=testEntry,ou=system" );

        collectiveAttributeSubentries = testEntry.get( "collectiveAttributeSubentries" );

        assertNull( collectiveAttributeSubentries );
    }

}
