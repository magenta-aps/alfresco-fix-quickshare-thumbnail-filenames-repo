package dk.magenta.alfresco.repo;


import org.alfresco.model.ContentModel;
import org.alfresco.repo.web.scripts.quickshare.QuickShareThumbnailContentGet;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.FilenameUtils;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import java.io.IOException;
import java.util.Map;

/**
 * Created by seth on 06/06/16.
 */
public class OverriddenQuickShareThumbnailContentGet extends
        QuickShareThumbnailContentGet {

    private ThreadLocal<NodeRef> originalNodeRef = new ThreadLocal<>();

    protected NodeService myNodeService;
    protected ContentService myContentService;

    public void setMyNodeService(NodeService myNodeService) {
        this.myNodeService = myNodeService;
    }

    public void setMyContentService(ContentService myContentService) {
        this.myContentService = myContentService;
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
}
