/*
 * Copyright 2008-2011 Don Brown
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
File buildLog = new File((String) basedir, "build.log")
def text = buildLog.getText()
text = text.replaceAll("\r\n", "\n")
return text.contains("     [echo] Mojo Executor ran successfully.");
