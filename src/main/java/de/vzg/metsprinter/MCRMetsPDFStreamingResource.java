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
package de.vzg.metsprinter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.jdom2.Document;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRPathContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.content.transformer.MCRFopper;
import org.mycore.common.content.transformer.MCRXSLTransformer;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.mets.model.MCRMETSGenerator;

@Path("/pdf")
public class MCRMetsPDFStreamingResource {

    private static final String PDF_FUNCTION_PREFIX = "MIR.PDF";

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
            Document mets = null;
            try {
                mets = MCRMETSGenerator.getGenerator().getMETS(MCRPath.getPath(derivate, "/"), ignoreNodes)
                    .asDocument();
            } catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                throw new MCRException("Error while generating mets.xml", e);
            }
            return new MCRJDOMContent(mets);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("/")
    public Response getRestrictions() {
        return Response.status(Response.Status.OK)
            .entity("{\"maxPages\": \"" + MCRConfiguration.instance().getString("MIR.PDF.MAXPages") + "\"}").build();
    }

    @GET
    @Path("/{derivate}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response streamPDF(@PathParam("derivate") String derivate, @QueryParam("pages") String pages,
        @DefaultValue("false") @QueryParam("test") boolean test) {
        if (!MCRConfiguration.instance().getBoolean(PDF_FUNCTION_PREFIX + ".Enabled", false)) {
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

        String stylesheet = MCRConfiguration.instance().getString(PDF_FUNCTION_PREFIX + ".XSLStylesheet");

        MCRSession session = MCRSessionMgr.getCurrentSession();
        session.put("derivateID", derivate);
        if (pages != null) {
            session.put("ranges", pages);
        }

        MCRXSLTransformer xslTransformer = new MCRXSLTransformer(stylesheet);

        ByteArrayOutputStream out;
        try {
            out = new ByteArrayOutputStream();
            xslTransformer.transform(metsContent, out, new MCRParameterCollector(false));
        } catch (IOException e) {
            throw new MCRException(e);
        }

        final ByteArrayOutputStream sout = out;
        Response.ResponseBuilder rb = Response.ok().entity(
            (StreamingOutput) outputStream -> {
                if (!test) {
                    new MCRFopper()
                        .transform(new MCRStreamContent(new ByteArrayInputStream(sout.toByteArray())), outputStream);
                } else {
                    outputStream.write(sout.toByteArray());
                }

            });
        if (!test) {
            rb = rb.header("Content-Disposition", "attachment; filename=\"" + derivate + ".pdf\"");
        }

        return rb.build();
    }

}
