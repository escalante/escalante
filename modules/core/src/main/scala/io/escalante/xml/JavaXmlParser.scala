/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.escalante.xml

import javax.xml.stream.{XMLStreamException, XMLStreamReader}
import javax.xml.stream.XMLStreamConstants._

/**
 * Java XML parser.
 *
 * @author Galder Zamarreño
 * @since 1.0
 */
object JavaXmlParser {

  /**
   * Get an exception reporting an unexpected XML element.
   * @param reader the stream reader
   * @return the exception
   */
  def unexpectedElement(reader: XMLStreamReader): XMLStreamException = {
    new XMLStreamException(
      s"Unexpected element '${reader.getName}' encountered", reader.getLocation)
  }

  /**
   * Consumes the remainder of the current element, throwing an
   * javax.xml.stream.XMLStreamException if it contains any child
   * elements.
   *
   * @param reader the reader
   * @throws javax.xml.stream.XMLStreamException if an error occurs
   */
  def requireNoContent(reader: XMLStreamReader) {
    if (reader.hasNext && reader.nextTag() != END_ELEMENT) {
      throw unexpectedElement(reader)
    }
  }

  /**
   * Read an element which contains only a single string attribute.
   *
   * @param reader the reader
   * @param attributeName the attribute name, usually "value" or "name"
   * @return the string value
   * @throws javax.xml.stream.XMLStreamException if an error occurs or if the
   *                                             element does not contain the specified attribute, contains other
   *                                             attributes, or contains child elements.
   */
  def readStringAttributeElement(reader: XMLStreamReader, attributeName: String): String = {
    requireSingleAttribute(reader, attributeName)
    val value = reader.getAttributeValue(0)
    requireNoContent(reader)
    value
  }

  /**
   * Require that the current element have only a single attribute with the given name.
   *
   * @param reader the reader
   * @param attributeName the attribute name
   * @throws XMLStreamException if an error occurs
   */
  def requireSingleAttribute(reader: XMLStreamReader, attributeName: String) {
    val count = reader.getAttributeCount
    if (count == 0) throw missingRequired(reader, Set(attributeName))

    if (attributeHasNamespace(reader, 0) ||
        !(attributeName == reader.getAttributeLocalName(0))) {
      throw unexpectedAttribute(reader, 0)
    }
    if (count > 1) {
      throw unexpectedAttribute(reader, 1)
    }
  }

  def readOptionalStringAttributeElement(reader: XMLStreamReader,
      attributeName: String): Option[String] = {
    val attributeValue = reader.getAttributeValue(null, attributeName)
    if (attributeValue != null) Some(attributeValue)
    else None
  }

  /**
   * Get an exception reporting a missing, required XML attribute.
   *
   * @param reader the stream reader
   * @param required a set of enums whose toString method returns the attribute name
   * @return the exception
   */
  def missingRequired(reader: XMLStreamReader, required: Set[Any]): XMLStreamException = {
    val b = new StringBuilder
    val iterator = required.iterator
    while (iterator.hasNext) {
      val o = iterator.next()
      b.append(o.toString)
      if (iterator.hasNext) {
        b.append(", ")
      }
    }
    new XMLStreamException(
      "Missing required attribute(s): " + b, reader.getLocation)
  }

  /**
   * Get an exception reporting an unexpected XML attribute.
   *
   * @param reader the stream reader
   * @param index the element index
   * @return the exception
   */
  def unexpectedAttribute(reader: XMLStreamReader, index: Int): XMLStreamException = {
    new XMLStreamException(
      s"Unexpected attribute '${reader.getAttributeName(index)}' encountered",
      reader.getLocation)
  }

  /**
   * TODO: Document
   */
  def attributeHasNamespace(reader: XMLStreamReader, i: Int): Boolean = {
    !(reader.getAttributeNamespace(i) == null
        || ("" == reader.getAttributeNamespace(i)))
  }

  /**
   * Checks that the current element has no attributes, throwing an
   * {@link javax.xml.stream.XMLStreamException} if one is found.
   * @param reader the reader
   * @throws javax.xml.stream.XMLStreamException if an error occurs
   */
  def requireNoAttributes(reader: XMLStreamReader) {
    if (reader.getAttributeCount() > 0) {
      throw unexpectedAttribute(reader, 0);
    }
  }


}