package dk.magenta.alfresco.repo;


import org.alfresco.model.ContentModel;
import org.alfresco.repo.web.scripts.content.ContentGet;
import org.alfresco.repo.web.scripts.quickshare.QuickShareContentGet;
import org.alfresco.repo.web.scripts.quickshare.QuickShareThumbnailContentGet;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.FilenameUtils;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by seth on 06/06/16.
 */
public class OverriddenThumbnailContentGet extends QuickShareThumbnailContentGet {

    private ThreadLocal<NodeRef> originalNodeRef = new ThreadLocal<>();

    protected NodeService myNodeService;
    protected ContentService myContentService;

    public void setMyNodeService(NodeService myNodeService) {
        this.myNodeService = myNodeService;
    }

    public void setMyContentService(ContentService myContentService) {
        this.myContentService = myContentService;
    }

    public void execute(WebScriptRequest req, WebScriptResponse res)
            throws IOException
    {
        // create map of args
        String[] names = req.getParameterNames();
        Map<String, String> args = new HashMap<String, String>(names.length, 1.0f);
        for (String name : names)
        {
            args.put(name, req.getParameter(name));
        }

        // create map of template vars
        Map<String, String> templateVars = req.getServiceMatch().getTemplateVars();

        // create object reference from url
        ObjectReference reference = createMyObjectReferenceFromUrl(args,
                templateVars);
        NodeRef nodeRef = reference.getNodeRef();
        if (nodeRef == null)
        {
            throw new WebScriptException(HttpServletResponse.SC_NOT_FOUND, "Unable to find " + reference.toString());
        }

        executeImpl(nodeRef, templateVars, req, res, null);
    }

    @Override
    protected void executeImpl(NodeRef nodeRef, Map<String, String> templateVars, WebScriptRequest req, WebScriptResponse res, Map<String, Object> model) throws IOException {
        // Store the nodeRef of the node that the thumbnail is being
        // requested for, for later on.
        originalNodeRef.set(nodeRef);
        super.executeImpl(nodeRef, templateVars, req, res, model);
    }

    @Override
    protected void streamContentLocal(WebScriptRequest req, WebScriptResponse res, NodeRef nodeRef, boolean attach, QName propertyQName, Map<String, Object> model) throws IOException {
        String userAgent = req.getHeader("User-Agent");
        userAgent = userAgent != null ? userAgent.toLowerCase() : "";
        boolean rfc5987Supported = (userAgent.contains("msie") || userAgent.contains(" trident/") || userAgent.contains(" chrome/") || userAgent.contains(" firefox/"));

        if (attach && rfc5987Supported) {
            // BEGIN MODIFICATION
            // Use the filename of the original nodeRef, NOT the
            // thumbnail's name.
            String name = (String) myNodeService.getProperty
                    (originalNodeRef.get(), ContentModel.PROP_NAME);

            // Set the filename's extension returned to be the file
            // extension of the thumbnail's mimetype.
            // e.g. for PDF thumbnails, return a .pdf file.
            String mimeType = myContentService.getReader(nodeRef,
                    propertyQName).getMimetype();
            name = FilenameUtils.removeExtension(name) + FilenameUtils.EXTENSION_SEPARATOR_STR + mimetypeService.getExtension(mimeType);
            // END MODIFICATION

            streamContent(req, res, nodeRef, propertyQName, attach, name, model);
        } else {
            streamContent(req, res, nodeRef, propertyQName, attach, null, model);
        }
    }


    protected ObjectReference createMyObjectReferenceFromUrl(Map<String,
            String> args, Map<String, String> templateArgs)
    {
        String objectId = args.get("noderef");
        if (objectId != null)
        {
            return new ObjectReference(objectId);
        }

        StoreRef storeRef = null;
        String store_type = templateArgs.get("store_type");
        String store_id = templateArgs.get("store_id");
        if (store_type != null && store_id != null)
        {
            storeRef = new StoreRef(store_type, store_id);
        }

        String id = templateArgs.get("id");
        if (storeRef != null && id != null)
        {
            return new ObjectReference(storeRef, id);
        }

        String nodepath = templateArgs.get("nodepath");
        if (nodepath == null)
        {
            nodepath = args.get("nodepath");
        }
        if (storeRef != null && nodepath != null)
        {
            return new ObjectReference(storeRef, nodepath.split("/"));
        }

        return null;
    }


    class ObjectReference
    {
        private NodeRef ref;

        ObjectReference(String nodeRef)
        {
            this.ref = new NodeRef(nodeRef);
        }

        ObjectReference(StoreRef ref, String id)
        {
            if (id.indexOf('/') != -1)
            {
                id = id.substring(0, id.indexOf('/'));
            }
            this.ref = new NodeRef(ref, id);
        }

        ObjectReference(StoreRef ref, String[] path)
        {
            String[] reference = new String[path.length + 2];
            reference[0] = ref.getProtocol();
            reference[1] = ref.getIdentifier();
            System.arraycopy(path, 0, reference, 2, path.length);
            this.ref = repository.findNodeRef("path", reference);
        }

        public NodeRef getNodeRef()
        {
            return this.ref;
        }

        @Override
        public String toString()
        {
            return ref != null ? ref.toString() : super.toString();
        }
    }

}
