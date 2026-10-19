package io.github.saqqqner.diagramkiln.internal;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import io.github.saqqqner.diagramkiln.DiagramData;
import io.github.saqqqner.diagramkiln.dto.LayoutResult;
import io.github.saqqqner.diagramkiln.dto.Point;
import io.github.saqqqner.diagramkiln.dto.PositionedNode;
import io.github.saqqqner.diagramkiln.dto.RoutedEdge;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.HashMap;

/**
 * Internal serializer: callers supply a validated input snapshot and page-coordinate geometry.
 */
public final class DrawioXmlWriter {
  /**
   * Writes a URI-safe #R fragment containing the complete generated mxfile document. The XML is
   * URI-encoded, raw-DEFLATE compressed and Base64 encoded before URI escaping.
   *
   * @param data
   *          validated input snapshot
   * @param layout
   *          page-coordinate geometry, validated before serialization
   * @return a fragment suitable for appending to a draw.io editor URL
   */
  public String writeFragment(DiagramData data, LayoutResult layout) {
    return writeFragment(write(data, layout));
  }

  /**
   * Encodes previously serialized XML without capturing input or running layout again.
   *
   * @param xml
   *          non-null serialized XML
   * @return a URI-safe #R fragment
   */
  public String writeFragment(String xml) {
    if (xml == null)
      throw new IllegalArgumentException("XML must not be null");
    return DrawioCompression.fragment(xml);
  }

  public String write(DiagramData data, LayoutResult layout) {
    GeometryValidator.validate(data, layout);
    var index = new DiagramIndex(data);
    try {
      var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      var file = append(document, "mxfile");
      file.setAttribute("compressed", "false");
      var page = append(file, "diagram");
      page.setAttribute("id", "page-1");
      page.setAttribute("name", data.sectionName());
      var model = append(page, "mxGraphModel");
      var root = append(model, "root");
      append(root, "mxCell").setAttribute("id", "0");
      var layer = append(root, "mxCell");
      layer.setAttribute("id", "1");
      layer.setAttribute("parent", "0");

      var nodes = new HashMap<String, PositionedNode>();
      layout.nodes().forEach(node -> nodes.put(node.id(), node));
      var xmlIds = new HashMap<String, String>();
      for (int i = 0; i < index.nodes().size(); i++) {
        var input = index.nodes().get(i);
        var node = nodes.get(input.id());
        String cellId = "node-" + i;
        xmlIds.put(input.id(), cellId);
        var cell = cell(root, cellId, input.label());
        String parentId = index.parentId(input.id());
        var parent = nodes.get(parentId);
        cell.setAttribute("parent", parentId == null ? "1" : xmlIds.get(parentId));
        cell.setAttribute("vertex", "1");
        cell.setAttribute("style", StyleWriter.node(input));
        var geometry = append(cell, "mxGeometry");
        geometry.setAttribute("as", "geometry");
        number(geometry, "x", node.x() - (parent == null ? 0 : parent.x()));
        number(geometry, "y", node.y() - (parent == null ? 0 : parent.y()));
        number(geometry, "width", node.width());
        number(geometry, "height", node.height());
      }
      var routes = new RoutedEdge[index.edges().size()];
      layout.edges().forEach(route -> routes[route.edgeIndex()] = route);
      for (int i = 0; i < index.edges().size(); i++) {
        var input = index.edges().get(i);
        var points = routes[i].points();
        var start = points.get(0);
        var end = points.get(points.size() - 1);
        var source = nodes.get(input.sourceId());
        var target = nodes.get(input.targetId());
        var cell = cell(root, "edge-" + i, input.label());
        String parentId = index.edgeParentId(input);
        var parent = nodes.get(parentId);
        cell.setAttribute("parent", parentId == null ? "1" : xmlIds.get(parentId));
        cell.setAttribute("edge", "1");
        cell.setAttribute("source", xmlIds.get(input.sourceId()));
        cell.setAttribute("target", xmlIds.get(input.targetId()));
        cell.setAttribute("style", StyleWriter.edge(input.style()) + anchors("exit", start, source)
            + anchors("entry", end, target));
        var geometry = append(cell, "mxGeometry");
        geometry.setAttribute("as", "geometry");
        geometry.setAttribute("relative", "1");
        if (points.size() > 2) {
          var array = append(geometry, "Array");
          array.setAttribute("as", "points");
          for (int p = 1; p < points.size() - 1; p++) {
            var point = append(array, "mxPoint");
            number(point, "x", points.get(p).x() - (parent == null ? 0 : parent.x()));
            number(point, "y", points.get(p).y() - (parent == null ? 0 : parent.y()));
          }
        }
      }
      return serialize(document);
    }
    catch (ParserConfigurationException | TransformerException exception) {
      throw new IllegalStateException("Could not generate draw.io XML", exception);
    }
  }

  private Element cell(Element root, String id, String label) {
    var cell = append(root, "mxCell");
    cell.setAttribute("id", id);
    cell.setAttribute("parent", "1");
    cell.setAttribute("value", label);
    return cell;
  }

  private String anchors(String prefix, Point point, PositionedNode node) {
    double x = Math.max(0, Math.min(1, (point.x() - node.x()) / node.width()));
    double y = Math.max(0, Math.min(1, (point.y() - node.y()) / node.height()));
    return prefix + "X=" + x + ";" + prefix + "Y=" + y + ";";
  }

  private Element append(org.w3c.dom.Node parent, String name) {
    Document document = parent instanceof Document doc ? doc : parent.getOwnerDocument();
    var child = document.createElement(name);
    parent.appendChild(child);
    return child;
  }

  private void number(Element element, String name, double value) {
    element.setAttribute(name, Double.toString(value));
  }

  private String serialize(Document document) throws TransformerException {
    var factory = TransformerFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    var transformer = factory.newTransformer();
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    var output = new StringWriter();
    transformer.transform(new DOMSource(document), new StreamResult(output));
    return output.toString();
  }
}
