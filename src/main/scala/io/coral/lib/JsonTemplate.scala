/*
 * Copyright 2016 Coral realtime streaming analytics (http://coral-streaming.github.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.coral.lib

import org.json4s.JsonAST._

object JsonTemplate {
	private def isReference(field: String): Boolean = {
		field.startsWith("${") && field.endsWith("}")
	}

	private def extractReference(field: String): String = {
		field.substring(2, field.length - 1).trim
	}

	private def parse(s: String, json: JObject): JValue = {
		val result = JsonExpressionParser.parse(s, json)
		if (result == JNothing) JNull
		else result
	}

	private def evaluate(json: JObject, node: JValue): JValue = node match {
		case JString(s) =>
			if (isReference(s)) parse(extractReference(s), json)
			else node
		case _ => node
	}

	def validate(template: JObject): Boolean = {
		// the only thing that could be checked would be expression syntax;
		// so until this is available from the JsonExpressionParser just say yea
		true
	}

	def apply(template: JObject): JsonTemplate = new JsonTemplate(template)
}

class JsonTemplate(val template: JObject) {
	def interpret(input: JObject): JValue = {
		def transformField(field: (String, JValue)): (String, JValue) =
			(field._1, JsonTemplate.evaluate(input, field._2))
		template.mapField(transformField)
	}
}