/**
 * Copyright (C) 2010 Orbeon, Inc.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
 */
package org.orbeon.oxf.xforms.state;

import org.orbeon.oxf.cache.Cache;
import org.orbeon.oxf.cache.InternalCacheKey;
import org.orbeon.oxf.cache.ObjectCache;
import org.orbeon.oxf.util.PropertyContext;
import org.orbeon.oxf.util.UUIDUtils;
import org.orbeon.oxf.xforms.XFormsContainingDocument;

/**
 * This cache stores mappings XFormsState -> XFormsContainingDocument into a global cache.
 */
public class XFormsDocumentCache {

    private static final String XFORMS_DOCUMENT_CACHE_NAME = "xforms.cache.documents";
    private static final int XFORMS_DOCUMENT_CACHE_DEFAULT_SIZE = 10;

    private static final Long CONSTANT_VALIDITY = (long) 0;
    private static final String CONTAINING_DOCUMENT_KEY_TYPE = XFORMS_DOCUMENT_CACHE_NAME;

    private static XFormsDocumentCache instance = new XFormsDocumentCache();

    public static XFormsDocumentCache instance() {
        return instance;
    }

    private final Cache cache = ObjectCache.instance(XFORMS_DOCUMENT_CACHE_NAME, XFORMS_DOCUMENT_CACHE_DEFAULT_SIZE);

    private XFormsDocumentCache() {}

    /**
     * Add a document to the cache using the document's UUID as cache key.
     *
     * @param propertyContext       current context
     * @param containingDocument    document to store
     */
    public void storeDocument(PropertyContext propertyContext, XFormsContainingDocument containingDocument) {
        final InternalCacheKey cacheKey = createCacheKey(containingDocument.getUUID());
        cache.add(propertyContext, cacheKey, CONSTANT_VALIDITY, containingDocument);
    }

    /**
     * Find a document in the cache. If not found, return null.
     *
     * @param propertyContext       current context
     * @param uuid                  UUID used to search cache
     * @return                      document or null
     */
    public XFormsContainingDocument getDocument(PropertyContext propertyContext, String uuid) {
        final InternalCacheKey cacheKey = createCacheKey(uuid);
        // NOTE: We used to remove the document from the cache, but this is no longer needed now that we store by UUID.
        return (XFormsContainingDocument) cache.findValid(propertyContext, cacheKey, CONSTANT_VALIDITY);
    }

    /**
     * Remove a document from the cache. This does not cause the document state to be serialized to store.
     *
     * @param propertyContext       current context
     * @param containingDocument    document to remove
     */
    public void removeDocument(PropertyContext propertyContext, XFormsContainingDocument containingDocument) {
        final InternalCacheKey cacheKey = createCacheKey(containingDocument.getUUID());
        cache.remove(propertyContext, cacheKey);
    }

    private InternalCacheKey createCacheKey(String uuid) {

        // Make sure that we are getting a UUID back
        assert uuid.length() == UUIDUtils.UUID_LENGTH;

        return new InternalCacheKey(CONTAINING_DOCUMENT_KEY_TYPE, uuid);
    }
}
