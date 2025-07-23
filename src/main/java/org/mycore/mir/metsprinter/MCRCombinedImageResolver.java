package org.mycore.mir.metsprinter;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.imagetiler.MCRImage;
import org.mycore.iview2.services.MCRIView2Tools;

public class MCRCombinedImageResolver implements URIResolver {

  public static final int MAX_ZOOM_LEVEL_FROM_TILE_COMBINE_SERVLET = 3;
  public static final String COMBINED_TILE_URI_PREFIX = "combinedTiles:";

  @Override
  public Source resolve(String href, String base) throws TransformerException {
    if(!href.startsWith(COMBINED_TILE_URI_PREFIX)) {
      throw new TransformerException("Invalid href prefix: " + href);
    }

    String cleanedHref = href.substring(COMBINED_TILE_URI_PREFIX.length());

    String[] split = cleanedHref.split("/", 2);
    if (split.length != 2) {
      throw new TransformerException("Invalid href: " + href);
    }

    String derivateID = split[0];
    String encodedImage = split[1];

    String imagePath = null;
    try {
      imagePath = MCRXMLFunctions.decodeURIPath(encodedImage);
    } catch (URISyntaxException e) {
      throw new TransformerException("Invalid image path in href: " + href, e);
    }

    MCRPath path = MCRPath.getPath(derivateID, imagePath);

    try {
      if(!MCRIView2Tools.isFileSupported(path)) {
        throw new TransformerException("File in path " + path + " is not supported for combined images.");
      }
    } catch (IOException e) {
      throw new MCRException("Error checking file support for path: " + path, e);
    }

    BufferedImage zoomLevel;
    Path tiledFile = MCRImage.getTiledFile(MCRIView2Tools.getTileDir(), derivateID, imagePath);
    try {
      zoomLevel = MCRIView2Tools.getZoomLevel(tiledFile,
          MAX_ZOOM_LEVEL_FROM_TILE_COMBINE_SERVLET);
    } catch (IOException e) {
      throw new MCRException("Error reading zoom level from tiled file: " + tiledFile, e);
    }

    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      ImageIO.write(zoomLevel, "png", os);
      InputStream is = new ByteArrayInputStream(os.toByteArray());
      return new StreamSource(is);
    } catch (IOException e) {
      throw new TransformerException("Failed to convert BufferedImage to StreamSource", e);
    }
  }
}
