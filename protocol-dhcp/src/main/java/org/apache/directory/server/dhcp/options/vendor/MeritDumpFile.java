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

package org.apache.directory.server.dhcp.options.vendor;


import org.apache.directory.server.dhcp.options.StringOption;


/**
 * This option specifies the path-name of a file to which the client's
 * core image should be dumped in the event the client crashes.  The
 * path is formatted as a character string consisting of characters from
 * the NVT ASCII character set.
 * 
 * The code for this option is 14.  Its minimum length is 1.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MeritDumpFile extends StringOption
{
    /*
     * @see org.apache.directory.server.dhcp.options.DhcpOption#getTag()
     */
    public byte getTag()
    {
        return 14;
    }
}
