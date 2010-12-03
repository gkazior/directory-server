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
package org.apache.directory.server.kerberos.protocol;


import java.io.IOException;

import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KerberosUdpEncoder extends ProtocolEncoderAdapter
{
    private KdcReplyEncoder replyEncoder = new KdcReplyEncoder();
    private ErrorMessageEncoder errorEncoder = new ErrorMessageEncoder();


    public void encode( IoSession session, Object message, ProtocolEncoderOutput out ) throws IOException
    {
        AbstractAsn1Object asn1Obj = ( AbstractAsn1Object ) message;
        
        IoBuffer buf = IoBuffer.allocate( asn1Obj.computeLength() );

        try
        {
            asn1Obj.encode( buf.buf() );

            buf.flip();

            out.write( buf );
        }
        catch( EncoderException e )
        {
            e.printStackTrace();
        }
        
//        if ( message instanceof KdcReply )
//        {
//            replyEncoder.encode( ( KdcReply ) message, buf.buf() );
//        }
//        else
//        {
//            if ( message instanceof ErrorMessage )
//            {
//                errorEncoder.encode( ( ErrorMessage ) message, buf.buf() );
//            }
//        }

    }
}