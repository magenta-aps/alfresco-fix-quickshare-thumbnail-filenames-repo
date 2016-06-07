Alfresco Repo module which modifies the quickshare thumbnail GET webscript to
send the filename of the thumbnail's source node (e.g. "my.pdf") instead of the
name of the thumbnail itself (e.g. "pdf" if the thumbnail is called "pdf") in
the Content-Disposition header, when the content is being requested as an
attachment (e.g. download).

This is so that the thumbnail can be downloaded by a browser
with an appropriate filename.
