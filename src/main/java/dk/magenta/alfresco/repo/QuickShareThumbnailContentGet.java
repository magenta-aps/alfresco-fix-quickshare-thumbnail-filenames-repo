package dk.magenta.alfresco.repo;


import org.alfresco.model.ContentModel;
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
public class QuickShareThumbnailContentGet extends
        org.alfresco.repo.web.scripts.quickshare
                .QuickShareThumbnailContentGet {

        protected NodeService myNodeService;
        protected ContentService myContentService;

        protected void streamContentLocal(WebScriptRequest req, WebScriptResponse res, NodeRef nodeRef, boolean attach, QName propertyQName, Map<String, Object> model) throws IOException
        {
                String userAgent = req.getHeader("User-Agent");
                userAgent = userAgent != null ? userAgent.toLowerCase() : "";
                boolean rfc5987Supported = (userAgent.contains("msie") || userAgent.contains(" trident/") || userAgent.contains(" chrome/") || userAgent.contains(" firefox/"));

                if (attach && rfc5987Supported)
                {
                        String name = (String) myNodeService.getProperty
                                (nodeRef, ContentModel.PROP_NAME);

                        // IE use file extension to get mimetype
                        // So we set correct extension. see MNT-11246
                        if (userAgent.contains("msie") || userAgent.contains(" trident/"))
                        {
                                String mimeType = myContentService.getReader(nodeRef,
                                        propertyQName).getMimetype();
                                if (!mimetypeService.getMimetypes(FilenameUtils.getExtension(name)).contains(mimeType))
                                {
                                        name = FilenameUtils.removeExtension(name) + FilenameUtils.EXTENSION_SEPARATOR_STR + mimetypeService.getExtension(mimeType);
                                }
                        }

                        streamContent(req, res, nodeRef, propertyQName, attach, name, model);
                }
                else
                {
                        streamContent(req, res, nodeRef, propertyQName, attach, null, model);
                }
        }

}
