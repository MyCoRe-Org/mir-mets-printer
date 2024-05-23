/*
 *  This file is part of ***  M y C o R e  ***
 *  See http://www.mycore.de/ for details.
 *
 *  This program is free software; you can use it, redistribute it
 *  and / or modify it under the terms of the GNU General Public License
 *  (GPL) as published by the Free Software Foundation; either version 2
 *  of the License or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program, in a file called gpl.txt or license.txt.
 *  If not, write to the Free Software Foundation Inc.,
 *  59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 */
package org.mycore.mir.metsprinter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.commons.io.output.CountingOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRPathContent;
import org.mycore.common.content.transformer.MCRXSLTransformer;
import org.mycore.common.events.MCRSessionEvent;
import org.mycore.common.events.MCRSessionListener;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.component.fo.common.fo.MCRFoFormatterHelper;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.mets.model.MCRMETSGeneratorFactory;

import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

@Path("/pdf")
    public class MCRMetsPDFStreamingResource implements MCRSessionListener {

    private static final String PDF_FUNCTION_PREFIX = "MIR.PDF";

    public static final String MIR_PDF_XSLSTYLESHEET = PDF_FUNCTION_PREFIX + ".XSLStylesheet";

    public static final String MIR_PDF_MAXPAGES = PDF_FUNCTION_PREFIX + ".MAXPages";

    public static final String METS_TEMP_FILE_SESSION_KEY = "mets2pdfTempFiles";

    private static final Logger LOGGER = LogManager.getLogger();

    @PostConstruct
    public void registerSessionListener(){
        LOGGER.info("Registering session listener for deleting temporary pdf files");
        MCRSessionMgr.addSessionListener(this);
    }

    private static MCRContent getMetsContent(String derivate) {
        MCRPath path = MCRPath.getPath(derivate, "mets.xml");
        boolean metsExists = Files.exists(path);
        if (metsExists) {
            MCRContent content = new MCRPathContent(path);
            content.setDocType("mets");
            return content;
        } else {
            HashSet<MCRPath> ignoreNodes = new HashSet<MCRPath>();
            if (metsExists) {
                ignoreNodes.add(path);
            }
            Document mets = MCRMETSGeneratorFactory.create(MCRPath.getPath(derivate, "/")).generate().asDocument();
            return new MCRJDOMContent(mets);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/")
    public Response getRestrictions() {
        return Response.status(Response.Status.OK)
            .entity("{\"maxPages\": \"" + MCRConfiguration2.getInt(MIR_PDF_MAXPAGES)
                .orElseThrow(() -> MCRConfiguration2.createConfigurationException(MIR_PDF_MAXPAGES)) + "\"}")
            .build();
    }

    @GET
    @Path("/{derivate}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response streamPDF(@PathParam("derivate") String derivate, @QueryParam("pages") String pages,
        @DefaultValue("false") @QueryParam("test") boolean test) {
        if (!MCRConfiguration2.getBoolean(PDF_FUNCTION_PREFIX + ".Enabled").orElse(false)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (!MCRAccessManager.checkPermission(derivate, MCRAccessManager.PERMISSION_READ)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        MCRObjectID derivateID = MCRObjectID.getInstance(derivate);
        if (!MCRMetadataManager.exists(derivateID)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        MCRContent metsContent = getMetsContent(derivate);

        String stylesheet = MCRConfiguration2.getString(MIR_PDF_XSLSTYLESHEET)
            .orElseThrow(() -> MCRConfiguration2.createConfigurationException(MIR_PDF_XSLSTYLESHEET));

        MCRSession session = MCRSessionMgr.getCurrentSession();
        session.put("derivateID", derivate);
        if (pages != null) {
            session.put("ranges", pages);
        }

        MCRXSLTransformer xslTransformer = new MCRXSLTransformer(stylesheet);

        byte[] byteArray;
        try(ByteArrayOutputStream out = new ByteArrayOutputStream() ) {
            xslTransformer.transform(metsContent, out, new MCRParameterCollector(false));
            byteArray = out.toByteArray();
        } catch (IOException e) {
            throw new MCRException(e);
        }

        java.nio.file.Path tempFile;
        long fileSize = -1;

        try {
            tempFile = Files.createTempFile("mets2pdf_" + derivate, ".pdf");
            storeTempFileInfoInSession(tempFile);
            try(OutputStream os = Files.newOutputStream(tempFile);
                CountingOutputStream cos = new CountingOutputStream(os)){
                if (!test) {
                    MCRFoFormatterHelper.getFoFormatter().transform(new MCRByteContent(byteArray), cos);
                } else {
                    cos.write(byteArray);
                }
                fileSize = cos.getByteCount();
            }
        } catch (IOException | TransformerException e) {
            throw new MCRException(e);
        }

        Response.ResponseBuilder rb = Response.ok().entity(
            (StreamingOutput) outputStream -> {
                try {
                    Files.copy(tempFile, outputStream);
                } catch (IOException e) {
                    throw new MCRException(e);
                }
                deleteFile(tempFile);
            });
        if (!test) {
            rb = rb.header("Content-Disposition", "attachment; filename=\"" + derivate + ".pdf\"");
        }
        if(fileSize > 0){
            rb.header("Content-Length", fileSize);
        }

        return rb.build();
    }

    private static void storeTempFileInfoInSession(java.nio.file.Path tempFile) {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        Object tempFileList = session.get(METS_TEMP_FILE_SESSION_KEY);
        if(tempFileList instanceof List list){
            list.add(tempFile);
        } else {
            List<java.nio.file.Path> list = new ArrayList<>();
            list.add(tempFile);
            session.put(METS_TEMP_FILE_SESSION_KEY, list);
        }
    }

    private static void deleteTempFilesInSession() {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        Object tempFileList = session.get(METS_TEMP_FILE_SESSION_KEY);
        if(tempFileList instanceof List list){
            for(Object tempFile : list){
                if(tempFile instanceof java.nio.file.Path tempFilePath) {
                    deleteFile(tempFilePath);
                }
            }
        }
    }

    private static void deleteFile(java.nio.file.Path tempFile) {
        try (OutputStream os = Files.newOutputStream(tempFile, StandardOpenOption.DELETE_ON_CLOSE)){
            // Workaround to delete the file after the response is sent
            os.close();
        } catch (IOException e) {
            throw new MCRException("Error while closing stream", e);
        }
    }

    @Override
    public void sessionEvent(MCRSessionEvent event) {
        if(event.getType().equals(MCRSessionEvent.Type.destroyed)){
            deleteTempFilesInSession();
        }
    }


}
