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
package org.apache.directory.server.core.normalization;


import org.apache.directory.api.ldap.model.cursor.EmptyCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.StringValue;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeTypeException;
import org.apache.directory.api.ldap.model.filter.AndNode;
import org.apache.directory.api.ldap.model.filter.BranchNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.NotNode;
import org.apache.directory.api.ldap.model.filter.ObjectClassNode;
import org.apache.directory.api.ldap.model.filter.OrNode;
import org.apache.directory.api.ldap.model.filter.PresenceNode;
import org.apache.directory.api.ldap.model.name.Ava;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.normalizers.ConcreteNameComponentNormalizer;
import org.apache.directory.api.ldap.model.schema.normalizers.NameComponentNormalizer;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InterceptorEnum;
import org.apache.directory.server.core.api.filtering.BaseEntryFilteringCursor;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.normalization.FilterNormalizingVisitor;
import org.apache.directory.server.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A name normalization service.  This service makes sure all relative and distinguished
 * names are normalized before calls are made against the respective interface methods
 * on {@link DefaultPartitionNexus}.
 *
 * The Filters are also normalized.
 *
 * If the Rdn AttributeTypes are not present in the entry for an Add request,
 * they will be added.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NormalizationInterceptor extends BaseInterceptor
{
    /** logger used by this class */
    private static final Logger LOG = LoggerFactory.getLogger( NormalizationInterceptor.class );

    /** a filter node value normalizer and undefined node remover */
    private FilterNormalizingVisitor normVisitor;


    /**
     * Creates a new instance of a NormalizationInterceptor.
     */
    public NormalizationInterceptor()
    {
        super( InterceptorEnum.NORMALIZATION_INTERCEPTOR );
    }


    /**
     * Initialize the registries, normalizers.
     */
    public void init( DirectoryService directoryService ) throws LdapException
    {
        LOG.debug( "Initialiazing the NormalizationInterceptor" );

        super.init( directoryService );

        NameComponentNormalizer ncn = new ConcreteNameComponentNormalizer( schemaManager );
        normVisitor = new FilterNormalizingVisitor( ncn, schemaManager );
    }


    /**
     * The destroy method does nothing
     */
    public void destroy()
    {
    }


    // ------------------------------------------------------------------------
    // Normalize all Name based arguments for ContextPartition interface operations
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public void add( AddOperationContext addContext ) throws LdapException
    {
        addContext.getDn().apply( schemaManager );
        addContext.getEntry().getDn().apply( schemaManager );
        addRdnAttributesToEntry( addContext.getDn(), addContext.getEntry() );
        next( addContext );
    }


    /**
     * {@inheritDoc}
     */
    public boolean compare( CompareOperationContext compareContext ) throws LdapException
    {
        compareContext.getDn().apply( schemaManager );

        // Get the attributeType from the OID
        try
        {
            AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( compareContext.getOid() );

            // Translate the value from binary to String if the AT is HR
            if ( attributeType.getSyntax().isHumanReadable() && ( !compareContext.getValue().isHumanReadable() ) )
            {
                String value = compareContext.getValue().getString();
                compareContext.setValue( new StringValue( value ) );
            }

            compareContext.setAttributeType( attributeType );
        }
        catch ( LdapException le )
        {
            throw new LdapInvalidAttributeTypeException( I18n.err( I18n.ERR_266, compareContext.getOid() ) );
        }

        return next( compareContext );
    }


    /**
     * {@inheritDoc}
     */
    public void delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        Dn dn = deleteContext.getDn();

        dn.apply( schemaManager );

        next( deleteContext );
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasEntry( HasEntryOperationContext hasEntryContext ) throws LdapException
    {
        hasEntryContext.getDn().apply( schemaManager );

        return next( hasEntryContext );
    }


    /**
     * {@inheritDoc}
     */
    public Entry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        lookupContext.getDn().apply( schemaManager );

        return next( lookupContext );
    }


    /**
     * {@inheritDoc}
     */
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        modifyContext.getDn().apply( schemaManager );

        if ( modifyContext.getModItems() != null )
        {
            for ( Modification modification : modifyContext.getModItems() )
            {
                AttributeType attributeType = schemaManager.getAttributeType( modification.getAttribute().getId() );
                modification.apply( attributeType );
            }
        }

        next( modifyContext );
    }


    /**
     * {@inheritDoc}
     */
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        moveContext.getDn().apply( schemaManager );
        moveContext.getOldSuperior().apply( schemaManager );
        moveContext.getNewSuperior().apply( schemaManager );
        moveContext.getNewDn().apply( schemaManager );

        if ( !moveContext.getRdn().isSchemaAware() )
        {
            moveContext.getRdn().apply( schemaManager );
        }

        next( moveContext );
    }


    /**
     * {@inheritDoc}
     */
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        if ( !moveAndRenameContext.getNewRdn().isSchemaAware() )
        {
            moveAndRenameContext.getNewRdn().apply( schemaManager );
        }

        moveAndRenameContext.getDn().apply( schemaManager );
        moveAndRenameContext.getNewDn().apply( schemaManager );
        moveAndRenameContext.getNewSuperiorDn().apply( schemaManager );

        next( moveAndRenameContext );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        // Normalize the new Rdn and the Dn if needed
        renameContext.getDn().apply( schemaManager );
        renameContext.getNewRdn().apply( schemaManager );
        renameContext.getNewDn().apply( schemaManager );

        // Push to the next interceptor
        next( renameContext );
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        Dn dn = searchContext.getDn();

        dn.apply( schemaManager );

        ExprNode filter = searchContext.getFilter();

        if ( filter == null )
        {
            LOG.warn( "undefined filter based on undefined attributeType not evaluted at all.  Returning empty enumeration." );
            return new BaseEntryFilteringCursor( new EmptyCursor<Entry>(), searchContext, schemaManager );
        }

        // Normalize the filter
        filter = ( ExprNode ) filter.accept( normVisitor );

        if ( filter == null )
        {
            LOG.warn( "undefined filter based on undefined attributeType not evaluted at all.  Returning empty enumeration." );
            return new BaseEntryFilteringCursor( new EmptyCursor<Entry>(), searchContext, schemaManager );
        }

        // We now have to remove the (ObjectClass=*) filter if it's present, and to add the scope filter
        ExprNode modifiedFilter = removeObjectClass( filter );

        searchContext.setFilter( modifiedFilter );

        // TODO Normalize the returned Attributes, storing the UP attributes to format the returned values.
        return next( searchContext );
    }


    /**
     * Remove the (ObjectClass=*) node from brancheNode, if we have one.
     */
    private ExprNode handleBranchNode( ExprNode node, BranchNode newBranchNode )
    {
        int nbNodes = 0;

        for ( ExprNode child : ( ( BranchNode ) node ).getChildren() )
        {
            ExprNode modifiedNode = removeObjectClass( child );

            if ( !( modifiedNode instanceof ObjectClassNode ) )
            {
                newBranchNode.addNode( modifiedNode );
                nbNodes++;
            }
        }

        switch ( nbNodes )
        {
            case 0:
                // Unlikely... But (&(ObjectClass=*)) is still an option 
                return ObjectClassNode.OBJECT_CLASS_NODE;

            case 1:
                if ( newBranchNode instanceof NotNode )
                {
                    return newBranchNode;
                }
                else
                {
                    // We can safely remove the AND/OR node and replace it with its first child
                    return newBranchNode.getFirstChild();
                }

            default:
                return newBranchNode;
        }
    }


    /**
     * Remove the (ObjectClass=*) node from the filter, if we have one.
     */
    private ExprNode removeObjectClass( ExprNode node )
    {
        if ( ( node instanceof PresenceNode ) &&
            ( ( ( PresenceNode ) node ).getAttributeType() == OBJECT_CLASS_AT ) )
        {
            // We can safely remove the node and return an undefined node
            return ObjectClassNode.OBJECT_CLASS_NODE;
        }
        // --------------------------------------------------------------------
        //                 H A N D L E   B R A N C H   N O D E S       
        // --------------------------------------------------------------------
        else if ( node instanceof AndNode )
        {
            AndNode newAndNode = new AndNode();

            return handleBranchNode( node, newAndNode );
        }
        else if ( node instanceof OrNode )
        {
            OrNode newOrNode = new OrNode();

            return handleBranchNode( node, newOrNode );
        }
        else if ( node instanceof NotNode )
        {
            NotNode newNotNode = new NotNode();

            return handleBranchNode( node, newNotNode );
        }
        else
        {
            // Failover : we return the initial node as is
            return node;
        }
    }


    // ------------------------------------------------------------------------
    // Normalize all Name based arguments for other interface operations
    // ------------------------------------------------------------------------
    /**
     * Adds missing Rdn's attributes and values to the entry.
     *
     * @param dn the Dn
     * @param entry the entry
     */
    private void addRdnAttributesToEntry( Dn dn, Entry entry ) throws LdapException
    {
        if ( dn == null || entry == null )
        {
            return;
        }

        Rdn rdn = dn.getRdn();

        // Loop on all the AVAs
        for ( Ava ava : rdn )
        {
            Value<?> value = ava.getNormValue();
            Value<?> upValue = ava.getValue();
            String upId = ava.getType();

            // Check that the entry contains this Ava
            if ( !entry.contains( upId, value ) )
            {
                String message = "The Rdn '" + upId + "=" + upValue + "' is not present in the entry";
                LOG.warn( message );

                // We don't have this attribute : add it.
                // Two cases :
                // 1) The attribute does not exist
                if ( !entry.containsAttribute( upId ) )
                {
                    entry.add( upId, upValue );
                }
                // 2) The attribute exists
                else
                {
                    AttributeType at = schemaManager.lookupAttributeTypeRegistry( upId );

                    // 2.1 if the attribute is single valued, replace the value
                    if ( at.isSingleValued() )
                    {
                        entry.removeAttributes( upId );
                        entry.add( upId, upValue );
                    }
                    // 2.2 the attribute is multi-valued : add the missing value
                    else
                    {
                        entry.add( upId, upValue );
                    }
                }
            }
        }
    }
}
