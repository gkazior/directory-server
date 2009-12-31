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
package org.apache.directory.server.operations.search;

 
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsResponseControl;

import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.shared.ldap.message.control.PagedSearchControl;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the PagedSearchControl. The following tables covers all the
 * possible cases for bothimport static org.junit.Assert.assertTrue;
 an admin and a simple user, combining the
 * Server SizeLimit (SL), the requested SizeLimit (SL) and the paged
 * size limit (PL). The 'X' column tells if we are supposed to receive
 * a SizeLimitExceededException.<br>
 * <br>
 * Administrator<br>
 * <pre>
 * +-------+----+----+----+---------------------+----+---+<br>
 * |test   | SL | RL | PL | Nb of responses     | nb | X |<br>
 * +-------+----+----+----+---------------------+----+---+<br>
 * |test1  | 0  | 0  | 3  | 4 ( 3 + 3 + 3 + 1 ) | 10 |   |<br>
 * |test2  | 0  | 0  | 5  | 2 ( 5 + 5 )         | 10 |   |<br>
 * |test3  | 3  | 0  | 5  | 2 ( 5 + 5 )         | 10 |   |<br>
 * |test4  | 0  | 3  | 5  | 1 ( 3 )             | 3  | Y |<br>
 * |test5  | 5  | 0  | 3  | 4 ( 3 + 3 + 3 + 1 ) | 10 |   |<br>
 * |test6  | 0  | 9  | 5  | 2 ( 5 + 4 )         | 5  | Y |<br>
 * |test7  | 5  | 0  | 5  | 2 ( 5 + 5 )         | 10 |   |<br>
 * |test8  | 0  | 5  | 5  | 1 ( 5 )             | 5  | Y |<br>
 * |test9  | 5  | 4  | 3  | 2 ( 3 + 1 )         | 4  | Y |<br>
 * |test10 | 4  | 5  | 3  | 2 ( 3 + 2 )         | 5  | Y |<br>
 * |test11 | 5  | 3  | 4  | 1 ( 3 )             | 3  | Y |<br>
 * |test12 | 5  | 4  | 3  | 2 ( 3 + 1 )         | 4  | Y |<br>
 * |test13 | 4  | 5  | 3  | 2 ( 3 + 2 )         | 5  | Y |<br>
 * |test14 | 4  | 3  | 5  | 1 ( 3 )             | 3  | Y |<br>
 * |test15 | 3  | 5  | 4  | 2 ( 4 + 1 )         | 5  | Y |<br>
 * |test16 | 3  | 4  | 5  | 1 ( 4 )             | 4  | Y |<br>
 * |test17 | 5  | 5  | 5  | 1 ( 5 )             | 5  | Y |<br>
 * +-------+----+----+----+---------------------+----+---+<br>
 * <br>
 * Simple user<br>
 * <br>
 * +-------+----+----+----+---------------------+----+---+<br>
 * |test   | SL | RL | PL | Nb of responses     | nb | X |<br>
 * +-------+----+----+----+---------------------+----+---+<br>
 * |test18 | 0  | 0  | 3  | 4 ( 3 + 3 + 3 + 1 ) | 10 |   |<br>
 * |test19  | 0  | 0  | 5  | 2 ( 5 + 5 )         | 10 |   |<br>
 * |test20  | 3  | 0  | 5  | 1 ( 3 )             | 3  | Y |<br>
 * |test21 | 0  | 3  | 5  | 1 ( 3 )             | 3  | Y |<br>
 * |test22 | 5  | 0  | 3  | 2 ( 3 + 2 )         | 5  | Y |<br>
 * |test23 | 0  | 9  | 5  | 2 ( 5 + 4 )         | 9  | Y |<br>
 * |test24 | 5  | 0  | 5  | 1 ( 5 )             | 5  | Y |<br>
 * |test25 | 0  | 5  | 5  | 1 ( 5 )             | 5  | Y |<br>
 * |test26 | 5  | 4  | 3  | 2 ( 3 + 1 )         | 4  | Y |<br>
 * |test27 | 4  | 5  | 3  | 2 ( 3 + 1 )         | 4  | Y |<br>
 * |test28 | 5  | 3  | 4  | 1 ( 3 )             | 3  | Y |<br>
 * |test29 | 5  | 4  | 3  | 2 ( 3 + 1 )         | 4  | Y |<br>
 * |test30 | 4  | 5  | 3  | 2 ( 3 + 1 )         | 4  | Y |<br>
 * |test31 | 4  | 3  | 5  | 1 ( 3 )             | 3  | Y |<br>
 * |test32 | 3  | 5  | 4  | 1 ( 3 )             | 3  | Y |<br>
 * |test33 | 3  | 4  | 5  | 1 ( 3 )             | 3  | Y |<br>
 * |test34 | 5  | 5  | 5  | 1 ( 5 )             | 5  | Y |<br>
 * +-------+----+----+----+---------------------+----+---+<br>
 *</pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 545029 $
 */
@RunWith ( FrameworkRunner.class )
@ApplyLdifs( {
    // Add 10 new entries
    "dn: dc=users,ou=system", 
    "objectClass: top", 
    "objectClass: domain", 
    "dc: users", 
    //
    "dn: cn=user0,dc=users,ou=system", 
    "objectClass: top", 
    "objectClass: person", 
    "cn: user0", 
    "sn: user 0", 
    //
    "dn: cn=user1,dc=users,ou=system", 
    "objectClass: top", 
    "objectClass: person", 
    "cn: user1", 
    "sn: user 1", 
    //
    "dn: cn=user2,dc=users,ou=system", 
    "objectClass: top", 
    "objectClass: person", 
    "cn: user2", 
    "sn: user 2", 
    //
    "dn: cn=user3,dc=users,ou=system", 
    "objectClass: top", 
    "objectClass: person", 
    "cn: user3", 
    "sn: user 3", 
    //
    "dn: cn=user4,dc=users,ou=system", 
    "objectClass: top", 
    "objectClass: person", 
    "cn: user4", 
    "sn: user 4", 
    //
    "dn: cn=user5,dc=users,ou=system", 
    "objectClass: top", 
    "objectClass: person", 
    "cn: user5", 
    "sn: user 5", 
    //
    "dn: cn=user6,dc=users,ou=system", 
    "objectClass: top", 
    "objectClass: person", 
    "cn: user6", 
    "sn: user 6", 
    //
    "dn: cn=user7,dc=users,ou=system", 
    "objectClass: top", 
    "objectClass: person", 
    "cn: user7", 
    "sn: user 7", 
    //
    "dn: cn=user8,dc=users,ou=system", 
    "objectClass: top", 
    "objectClass: person", 
    "cn: user8", 
    "sn: user 8", 
    // 
    "dn: cn=user9,dc=users,ou=system", 
    "objectClass: top", 
    "objectClass: person", 
    "cn: user9", 
    "sn: user 9", 
    "", 
    // Add another user for non admin tests
    "dn: cn=user,ou=system", 
    "objectClass: top", 
    "objectClass: person", 
    "cn: user", 
    "userPassword: secret", 
    "sn: user"
    }
)
public class PagedSearchIT extends AbstractLdapTestUnit
{
    /**
     * Create the searchControls with a paged size
     */
    private SearchControls createSearchControls( DirContext ctx, int sizeLimit, int pagedSize ) 
        throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setCountLimit( sizeLimit );
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        PagedSearchControl pagedSearchControl = new PagedSearchControl();
        pagedSearchControl.setSize( pagedSize );
        
        ((LdapContext)ctx).setRequestControls( new Control[] {pagedSearchControl} );
        
        return controls;
    }
    
    
    /**
     * Create the searchControls with a paged size
     */
    private void createNextSearchControls( DirContext ctx, byte[] cookie, int pagedSize ) 
        throws NamingException
    {
        PagedSearchControl pagedSearchControl = new PagedSearchControl();
        pagedSearchControl.setCookie( cookie );
        pagedSearchControl.setSize( pagedSize );
        ((LdapContext)ctx).setRequestControls( new Control[] {pagedSearchControl} );
    }
    
    
    /**
     * Check that we got the correct result set
     */
    private void checkResults( List<SearchResult> results, int expectedSize ) throws NamingException
    {
        assertEquals( expectedSize, results.size() );
        
        // check that we have correctly read all the entries
        for ( int i = 0; i < expectedSize; i++ )
        {
            SearchResult entry = results.get( i );
            assertEquals( "user" + i, entry.getAttributes().get( "cn" ).get() );
        }
    }
    
    
    /**
     * Do the loop over the entries, until we can't get any more, or until we
     * reach a limit. It will check that we have got all the expected entries.
     */
    private void doLoop( DirContext ctx, SearchControls controls, int pagedSizeLimit, 
        int expectedLoop, int expectedNbEntries, boolean expectedException ) throws NamingException
    {
        // Loop over all the elements
        int loop = 0;
        boolean hasSizeLimitException = false;
        List<SearchResult> results = new ArrayList<SearchResult>();
        
        while ( true )
        {
            loop++;

            try
            {
                NamingEnumeration<SearchResult> list = 
                    ctx.search( "dc=users,ou=system", "(cn=*)", controls );
    
                while ( list.hasMore() )
                {
                    SearchResult result = list.next();
                    results.add( result );
                }
            }
            catch ( Exception e )
            {
                hasSizeLimitException = true;
                break;
            }

            // Now read the next ones
            Control[] responseControls = ((LdapContext)ctx).getResponseControls();
            
            PagedResultsResponseControl responseControl = 
                (PagedResultsResponseControl)responseControls[0];
            assertEquals( 0, responseControl.getResultSize() );
            
            // check if this is over
            byte[] cookie = responseControl.getCookie();
            
            if ( StringTools.isEmpty( cookie ) )
            {
                // If so, exit the loop
                break;
            }
            
            // Prepare the next iteration
            createNextSearchControls( ctx, responseControl.getCookie(), pagedSizeLimit );
        }
        
        assertEquals( expectedException, hasSizeLimitException );
        assertEquals( expectedLoop, loop );
        checkResults( results, expectedNbEntries );
    }
    
    
    /**
     * Clear the server SizeLimit
     */
    @Before
    public void reinitLdapServiceMaxSizeLimit()
    {
        ldapServer.setMaxSizeLimit( LdapServer.NO_SIZE_LIMIT );
    }
    
    
    /**
     * Admin = yes <br>
     * SL = none<br>
     * RL = none<br>
     * PL = 3<br>
     * expected exception : no<br>
     * expected number of entries returned : 10 ( 3 + 3 + 3 + 1 )<br>
     */
    @Test
    public void testPagedSearchtest1() throws Exception
    {
        DirContext ctx = getWiredContext( ldapServer );
        SearchControls controls = createSearchControls( ctx, LdapServer.NO_SIZE_LIMIT, 3 );
        
        doLoop( ctx, controls, 3, 4, 10, false );
    }
    
    
    /**
     * Admin = yes <br>
     * SL = none<br>
     * RL = none<br>
     * PL = 5<br>
     * expected exception : no<br>
     * expected number of entries returned : 10 ( 5 + 5 )<br>
     */
    @Test
    public void testPagedSearchtest2() throws Exception
    {
        DirContext ctx = getWiredContext( ldapServer );
        SearchControls controls = createSearchControls( ctx, LdapServer.NO_SIZE_LIMIT, 5 );
        
        doLoop( ctx, controls, 5, 2, 10, false );
    }
    
    
    /**
     * Admin = yes <br>
     * SL = 3<br>
     * RL = none<br>
     * PL = 5<br>
     * expected exception : no<br>
     * expected number of entries returned : 10 ( 5 + 5 )<br>
     */
    @Test
    public void testPagedSearchTest3() throws Exception
    {
        ldapServer.setMaxSizeLimit( 3 );
        DirContext ctx = getWiredContext( ldapServer );
        SearchControls controls = createSearchControls( ctx, LdapServer.NO_SIZE_LIMIT, 5 );
        
        doLoop( ctx, controls, 5, 2, 10, false );
    }
    
    
    /**
     * Admin = yes <br>
     * SL = none<br>
     * RL = 3<br>
     * PL = 5<br>
     * expected exception : yes<br>
     * expected number of entries returned : 3<br>
     */
    @Test
    public void testPagedSearchTest4() throws Exception
    {
        DirContext ctx = getWiredContext( ldapServer );
        SearchControls controls = createSearchControls( ctx, 3, 5 );
        
        doLoop( ctx, controls, 5, 1, 3, true );
    }
    
    
    /**
     * Admin = yes <br>
     * SL = 5<br>
     * RL = none<br>
     * PL = 3<br>
     * expected exception : no<br>
     * expected number of entries returned : 10 ( 3 + 3 + 3 + 1 )<br>
     */
    @Test
    public void testPagedSearchtest5() throws Exception
    {
        ldapServer.setMaxSizeLimit( 5 );
        DirContext ctx = getWiredContext( ldapServer );
        SearchControls controls = createSearchControls( ctx, LdapServer.NO_SIZE_LIMIT, 3 );

        doLoop( ctx, controls, 3, 4, 10, false );
    }
    
    
    /**
     * Admin = yes <br>
     * SL = none<br>
     * RL = 9<br>
     * PL = 5<br>
     * expected exception : yes<br>
     * expected number of entries returned : 9 ( 5 + 4 )<br>
     */
    @Test
    public void testPagedSearchTest6() throws Exception
    {
        DirContext ctx = getWiredContext( ldapServer );
        SearchControls controls = createSearchControls( ctx, 9, 5 );
        
        doLoop( ctx, controls, 5, 2, 9, true );
    }
    
    
    /**
     * Admin = yes <br>
     * SL = 5<br>
     * RL = none<br>
     * PL = 5<br>
     * expected exception : no<br>
     * expected number of entries returned : 10 ( 5 + 5 )<br>
     */
    @Test
    public void testPagedSearchtest7() throws Exception
    {
        ldapServer.setMaxSizeLimit( 5 );
        DirContext ctx = getWiredContext( ldapServer );
        SearchControls controls = createSearchControls( ctx, LdapServer.NO_SIZE_LIMIT, 5 );
        
        doLoop( ctx, controls, 5, 2, 10, false );
    }
    
    
    /**
     * Admin = yes <br>
     * SL = none<br>
     * RL = 5<br>
     * PL = 5<br>
     * expected exception : yes<br>
     * expected number of entries returned : 5<br>
     */
    @Test
    public void testPagedSearchTest8() throws Exception
    {
        DirContext ctx = getWiredContext( ldapServer );
        SearchControls controls = createSearchControls( ctx, 5, 5 );
        
        doLoop( ctx, controls, 5, 1, 5, true );
    }
    
    
    /**
     * Admin = yes <br>
     * SL = 5<br>
     * RL = 4<br>
     * PL = 3<br>
     * expected exception : yes<br>
     * expected number of entries returned : 2 ( 3 + 1 )<br>
     */
    @Test
    public void testPagedSearchTest9() throws Exception
    {
        ldapServer.setMaxSizeLimit( 5 );
        DirContext ctx = getWiredContext( ldapServer );
        SearchControls controls = createSearchControls( ctx, 4, 3 );
        
        doLoop( ctx, controls, 3, 2, 4, true );
    }
    
    
    /**
     * Admin = yes <br>
     * SL = 4<br>
     * RL = 5<br>
     * PL = 3<br>
     * expected exception : yes<br>
     * expected number of entries returned : 5 ( 3 + 2 )<br>
     */
    @Test
    public void testPagedSearchtest10() throws Exception
    {
        ldapServer.setMaxSizeLimit( 4 );
        DirContext ctx = getWiredContext( ldapServer );
        SearchControls controls = createSearchControls( ctx, 5, 3 );
        
        doLoop( ctx, controls, 3, 2, 5, true );
    }
    
    
    /**
     * Admin = yes <br>
     * SL = 5<br>
     * RL = 3<br>
     * PL = 4<br>
     * expected exception : yes<br>
     * expected number of entries returned : 3<br>
     */
    @Test
    public void testPagedSearchtest11() throws Exception
    {
        ldapServer.setMaxSizeLimit( 5 );
        DirContext ctx = getWiredContext( ldapServer );
        SearchControls controls = createSearchControls( ctx, 3, 4 );
        
        doLoop( ctx, controls, 4, 1, 3, true );
    }
    
    
    /**
     * Admin = yes <br>
     * SL = 5<br>
     * RL = 4<br>
     * PL = 3<br>
     * expected exception : yes<br>
     * expected number of entries returned : 4 ( 3 + 1 )<br>
     */
    @Test
    public void testPagedSearchtest12() throws Exception
    {
        ldapServer.setMaxSizeLimit( 5 );
        DirContext ctx = getWiredContext( ldapServer );
        SearchControls controls = createSearchControls( ctx, 4, 3 );
        
        doLoop( ctx, controls, 3, 2, 4, true );
    }
    
    
    /**
     * Admin = yes <br>
     * SL = 4<br>
     * RL = 5<br>
     * PL = 3<br>
     * expected exception : yes<br>
     * expected number of entries returned : 5 ( 3 + 2 )<br>
     */
    @Test
    public void testPagedSearchtest13() throws Exception
    {
        ldapServer.setMaxSizeLimit( 4 );
        DirContext ctx = getWiredContext( ldapServer );
        SearchControls controls = createSearchControls( ctx, 5, 3 );
        
        doLoop( ctx, controls, 3, 2, 5, true );
    }
    
    
    /**
     * Admin = yes <br>
     * SL = 4<br>
     * RL = 3<br>
     * PL = 5<br>
     * expected exception : yes<br>
     * expected number of entries returned : 3 <br>
     */
    @Test
    public void testPagedSearchtest14() throws Exception
    {
        ldapServer.setMaxSizeLimit( 4 );
        DirContext ctx = getWiredContext( ldapServer );
        SearchControls controls = createSearchControls( ctx, 3, 5 );
        
        doLoop( ctx, controls, 5, 1, 3, true );
    }

    
    /**
     * Admin = yes <br>
     * SL = 3<br>
     * RL = 5<br>
     * PL = 4<br>
     * expected exception : yes<br>
     * expected number of entries returned : 5 ( 4 + 1 )<br>
     */
    @Test
    public void testPagedSearchtest15() throws Exception
    {
        ldapServer.setMaxSizeLimit( 3 );
        DirContext ctx = getWiredContext( ldapServer );
        SearchControls controls = createSearchControls( ctx, 5, 4 );
        
        doLoop( ctx, controls, 4, 2, 5, true );
    }

    
    /**
     * Admin = yes <br>
     * SL = 3<br>
     * RL = 4<br>
     * PL = 5<br>
     * expected exception : yes<br>
     * expected number of entries returned : 4 <br>
     */
    @Test
    public void testPagedSearchtest16() throws Exception
    {
        ldapServer.setMaxSizeLimit( 3 );
        DirContext ctx = getWiredContext( ldapServer );
        SearchControls controls = createSearchControls( ctx, 4, 5 );
        
        doLoop( ctx, controls, 5, 1, 4, true );
    }

    
    /**
     * Admin = yes <br>
     * SL = 5<br>
     * RL = 5<br>
     * PL = 5<br>
     * expected exception : yes<br>
     * expected number of entries returned : 5 <br>
     */
    @Test
    public void testPagedSearchtest17() throws Exception
    {
        ldapServer.setMaxSizeLimit( 5 );
        DirContext ctx = getWiredContext( ldapServer );
        SearchControls controls = createSearchControls( ctx, 5, 5 );
        
        doLoop( ctx, controls, 5, 1, 5, true );
    }
    
    
    /**
     * Admin = no <br>
     * SL = none<br>
     * RL = none<br>
     * PL = 3<br>
     * expected exception : no<br>
     * expected number of entries returned : 10 ( 3 + 3 + 3 + 1 )<br>
     */
    @Test
    public void testPagedSearchtest18() throws Exception
    {
        DirContext ctx = getWiredContext( ldapServer, "cn=user,ou=system", "secret" );
        SearchControls controls = createSearchControls( ctx, LdapServer.NO_SIZE_LIMIT, 3 );
        
        doLoop( ctx, controls, 3, 4, 10, false );
    }
    
    
    /**
     * Admin = no <br>
     * SL = none<br>
     * RL = none<br>
     * PL = 5<br>
     * expected exception : no<br>
     * expected number of entries returned : 10 ( 5 + 5 )<br>
     */
    @Test
    public void testPagedSearchtest19() throws Exception
    {
        DirContext ctx = getWiredContext( ldapServer, "cn=user,ou=system", "secret" );
        SearchControls controls = createSearchControls( ctx, LdapServer.NO_SIZE_LIMIT, 5 );
        
        doLoop( ctx, controls, 5, 2, 10, false );
    }
    
    
    /**
     * Admin = no <br>
     * SL = 3<br>
     * RL = none<br>
     * PL = 5<br>
     * expected exception : yes<br>
     * expected number of entries returned : 3<br>
     */
    @Test
    public void testPagedSearchTest20() throws Exception
    {
        ldapServer.setMaxSizeLimit( 3 );
        DirContext ctx = getWiredContext( ldapServer, "cn=user,ou=system", "secret" );
        SearchControls controls = createSearchControls( ctx, LdapServer.NO_SIZE_LIMIT, 5 );
        
        doLoop( ctx, controls, 5, 1, 3, true );
    }
    
    
    /**
     * Admin = no <br>
     * SL = none<br>
     * RL = 3<br>
     * PL = 5<br>
     * expected exception : yes<br>
     * expected number of entries returned : 3<br>
     */
    @Test
    public void testPagedSearchTest21() throws Exception
    {
        DirContext ctx = getWiredContext( ldapServer, "cn=user,ou=system", "secret" );
        SearchControls controls = createSearchControls( ctx, 3, 5 );
        
        doLoop( ctx, controls, 5, 1, 3, true );
    }
    
    
    /**
     * Admin = no <br>
     * SL = 5<br>
     * RL = none<br>
     * PL = 3<br>
     * expected exception : yes<br>
     * expected number of entries returned : 5 ( 3 + 2 )<br>
     */
    @Test
    public void testPagedSearchtest22() throws Exception
    {
        ldapServer.setMaxSizeLimit( 5 );
        DirContext ctx = getWiredContext( ldapServer, "cn=user,ou=system", "secret" );
        SearchControls controls = createSearchControls( ctx, LdapServer.NO_SIZE_LIMIT, 3 );
        
        doLoop( ctx, controls, 3, 2, 5, true );
    }
    
    
    /**
     * Admin = no <br>
     * SL = none<br>
     * RL = 9<br>
     * PL = 5<br>
     * expected exception : yes<br>
     * expected number of entries returned : 9 ( 5 + 4 )<br>
     */
    @Test
    public void testPagedSearchTest23() throws Exception
    {
        DirContext ctx = getWiredContext( ldapServer, "cn=user,ou=system", "secret" );
        SearchControls controls = createSearchControls( ctx, 9, 5 );
        
        
        doLoop( ctx, controls, 5, 2, 9, true );
    }
    
    
    /**
     * Admin = no <br>
     * SL = 5<br>
     * RL = none<br>
     * PL = 5<br>
     * expected exception : yes<br>
     * expected number of entries returned : 5<br>
     */
    @Test
    public void testPagedSearchtest24() throws Exception
    {
        ldapServer.setMaxSizeLimit( 5 );
        DirContext ctx = getWiredContext( ldapServer, "cn=user,ou=system", "secret" );
        SearchControls controls = createSearchControls( ctx, LdapServer.NO_SIZE_LIMIT, 5 );
        
        doLoop( ctx, controls, 5, 1, 5, true );
    }
    
    
    /**
     * Admin = no <br>
     * SL = none<br>
     * RL = 5<br>
     * PL = 5<br>
     * expected exception : yes<br>
     * expected number of entries returned : 5<br>
     */
    @Test
    public void testPagedSearchTest25() throws Exception
    {
        DirContext ctx = getWiredContext( ldapServer, "cn=user,ou=system", "secret" );
        SearchControls controls = createSearchControls( ctx, 5, 5 );
        
        
        doLoop( ctx, controls, 5, 1, 5, true );
    }
    
    
    /**
     * Admin = no <br>
     * SL = 5<br>
     * RL = 4<br>
     * PL = 3<br>
     * expected exception : yes<br>
     * expected number of entries returned : 2 ( 3 + 1 )<br>
     */
    @Test
    public void testPagedSearchTest26() throws Exception
    {
        ldapServer.setMaxSizeLimit( 5 );
        DirContext ctx = getWiredContext( ldapServer, "cn=user,ou=system", "secret" );
        SearchControls controls = createSearchControls( ctx, 4, 3 );
        
        doLoop( ctx, controls, 3, 2, 4, true );
    }
    
    
    /**
     * Admin = no <br>
     * SL = 4<br>
     * RL = 5<br>
     * PL = 3<br>
     * expected exception : yes<br>
     * expected number of entries returned : 4 ( 3 + 1 )<br>
     */
    @Test
    public void testPagedSearchtest27() throws Exception
    {
        ldapServer.setMaxSizeLimit( 4 );
        DirContext ctx = getWiredContext( ldapServer, "cn=user,ou=system", "secret" );
        SearchControls controls = createSearchControls( ctx, 5, 3 );
        
        doLoop( ctx, controls, 3, 2, 4, true );
    }
    
    
    /**
     * Admin = no <br>
     * SL = 5<br>
     * RL = 3<br>
     * PL = 4<br>
     * expected exception : yes<br>
     * expected number of entries returned : 3<br>
     */
    @Test
    public void testPagedSearchtest28() throws Exception
    {
        ldapServer.setMaxSizeLimit( 5 );
        DirContext ctx = getWiredContext( ldapServer, "cn=user,ou=system", "secret" );
        SearchControls controls = createSearchControls( ctx, 3, 4 );
        
        doLoop( ctx, controls, 4, 1, 3, true );
    }
    
    
    /**
     * Admin = no <br>
     * SL = 5<br>
     * RL = 4<br>
     * PL = 3<br>
     * expected exception : yes<br>
     * expected number of entries returned : 4 ( 3 + 1 )<br>
     */
    @Test
    public void testPagedSearchtest29() throws Exception
    {
        ldapServer.setMaxSizeLimit( 5 );
        DirContext ctx = getWiredContext( ldapServer, "cn=user,ou=system", "secret" );
        SearchControls controls = createSearchControls( ctx, 4, 3 );
        
        doLoop( ctx, controls, 3, 2, 4, true );
    }
    
    
    /**
     * Admin = no <br>
     * SL = 4<br>
     * RL = 5<br>
     * PL = 3<br>
     * expected exception : yes<br>
     * expected number of entries returned : 4 ( 3 + 1 )<br>
     */
    @Test
    public void testPagedSearchtest30() throws Exception
    {
        ldapServer.setMaxSizeLimit( 4 );
        DirContext ctx = getWiredContext( ldapServer, "cn=user,ou=system", "secret" );
        SearchControls controls = createSearchControls( ctx, 5, 3 );
        
        doLoop( ctx, controls, 3, 2, 4, true );
    }
    
    
    /**
     * Admin = no <br>
     * SL = 4<br>
     * RL = 3<br>
     * PL = 5<br>
     * expected exception : yes<br>
     * expected number of entries returned : 3 <br>
     */
    @Test
    public void testPagedSearchtest31() throws Exception
    {
        ldapServer.setMaxSizeLimit( 4 );
        DirContext ctx = getWiredContext( ldapServer, "cn=user,ou=system", "secret" );
        SearchControls controls = createSearchControls( ctx, 3, 5 );
        
        doLoop( ctx, controls, 3, 1, 3, true );
    }

    
    /**
     * Admin = no <br>
     * SL = 3<br>
     * RL = 5<br>
     * PL = 4<br>
     * expected exception : yes<br>
     * expected number of entries returned : 3 <br>
     */
    @Test
    public void testPagedSearchtest32() throws Exception
    {
        ldapServer.setMaxSizeLimit( 3 );
        DirContext ctx = getWiredContext( ldapServer, "cn=user,ou=system", "secret" );
        SearchControls controls = createSearchControls( ctx, 5, 4 );
        
        doLoop( ctx, controls, 3, 1, 3, true );
    }

    
    /**
     * Admin = no <br>
     * SL = 3<br>
     * RL = 4<br>
     * PL = 5<br>
     * expected exception : yes<br>
     * expected number of entries returned : 3 <br>
     */
    @Test
    public void testPagedSearchtest33() throws Exception
    {
        ldapServer.setMaxSizeLimit( 3 );
        DirContext ctx = getWiredContext( ldapServer, "cn=user,ou=system", "secret" );
        SearchControls controls = createSearchControls( ctx, 4, 5 );
        
        doLoop( ctx, controls, 3, 1, 3, true );
    }

    
    /**
     * Admin = no <br>
     * SL = 5<br>
     * RL = 5<br>
     * PL = 5<br>
     * expected exception : yes<br>
     * expected number of entries returned : 5 <br>
     */
    @Test
    public void testPagedSearchtest34() throws Exception
    {
        ldapServer.setMaxSizeLimit( 5 );
        DirContext ctx = getWiredContext( ldapServer, "cn=user,ou=system", "secret" );
        SearchControls controls = createSearchControls( ctx, 5, 5 );
        
        doLoop( ctx, controls, 5, 1, 5, true );
    }

    
    /**
     * Admin = no <br>
     * SL = none<br>
     * RL = none<br>
     * PL = -2<br>
     * expected exception : no<br>
     * expected number of entries returned : 10 <br>
     */
    @Test
    public void testPagedSearchWithNegativePL() throws Exception
    {
        DirContext ctx = getWiredContext( ldapServer, "cn=user,ou=system", "secret" );
        SearchControls controls = createSearchControls( ctx, LdapServer.NO_SIZE_LIMIT, -2 );
        
        doLoop( ctx, controls, -2, 1, 10, false );
    }
    
    
    /**
     * Do a test with a paged search and send a wrong cookie in the middle
     */
    @Test 
    public void testPagedSearchWrongCookie() throws Exception
    {
        DirContext ctx = getWiredContext( ldapServer, "cn=user,ou=system", "secret" );
        SearchControls controls = createSearchControls( ctx, LdapServer.NO_SIZE_LIMIT, 3 );

        // Loop over all the elements
        int loop = 0;
        List<SearchResult> results = new ArrayList<SearchResult>();
        boolean hasUnwillingToPerform = false;
        
        while ( true )
        {
            loop++;

            try
            {
                NamingEnumeration<SearchResult> list = 
                    ctx.search( "dc=users,ou=system", "(cn=*)", controls );
    
                while ( list.hasMore() )
                {
                    SearchResult result = list.next();
                    results.add( result );
                }
            }
            catch ( OperationNotSupportedException onse )
            {
                hasUnwillingToPerform = true;
                break;
            }

            // Now read the next ones
            Control[] responseControls = ((LdapContext)ctx).getResponseControls();
            
            PagedResultsResponseControl responseControl = 
                (PagedResultsResponseControl)responseControls[0];
            assertEquals( 0, responseControl.getResultSize() );
            
            // check if this is over
            byte[] cookie = responseControl.getCookie();
            
            if ( StringTools.isEmpty( cookie ) )
            {
                // If so, exit the loop
                break;
            }
            
            // Prepare the next iteration, sending a bad cookie
            createNextSearchControls( ctx, "test".getBytes( "UTF-8" ), 3 );
        }
        
        assertTrue( hasUnwillingToPerform );
    }

    
    
    /**
     * Do a test with a paged search, changing the number of entries to 
     * return in the middle of the loop
     */
    @Test 
    public void testPagedSearchModifyingPagedLimit() throws Exception
    {
        DirContext ctx = getWiredContext( ldapServer, "cn=user,ou=system", "secret" );
        SearchControls controls = createSearchControls( ctx, LdapServer.NO_SIZE_LIMIT, 4 );

        // Loop over all the elements
        int loop = 0;
        List<SearchResult> results = new ArrayList<SearchResult>();
        
        // The expected size after each loop.
        int[] expectedSize = new int[]{4, 7, 9, 10};
        
        while ( true )
        {
            loop++;

            NamingEnumeration<SearchResult> list = 
                ctx.search( "dc=users,ou=system", "(cn=*)", controls );

            while ( list.hasMore() )
            {
                SearchResult result = list.next();
                results.add( result );
            }

            // Now read the next ones
            Control[] responseControls = ((LdapContext)ctx).getResponseControls();
            
            PagedResultsResponseControl responseControl = 
                (PagedResultsResponseControl)responseControls[0];
            assertEquals( 0, responseControl.getResultSize() );
            
            // check if this is over
            byte[] cookie = responseControl.getCookie();
            
            if ( StringTools.isEmpty( cookie ) )
            {
                // If so, exit the loop
                break;
            }
            
            // Prepare the next iteration, sending a bad cookie
            createNextSearchControls( ctx, responseControl.getCookie(), 4-loop );
            
            assertEquals( expectedSize[loop-1], results.size() );
        }
        
        assertEquals( 4, loop );
        checkResults( results, 10 );
    }
}