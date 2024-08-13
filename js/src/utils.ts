/*
    Copyright 2019-2020 eMobilify GmbH

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
/**
 * takes a utf-8 encoded string and returns it as a hex string
 * @param str utf-8 encoded string
 * @returns hex string of input
 */
export function toHex(str: string): string {
    if (str.startsWith("0x")) {
        throw Error("got hex string, want utf-8 string")
    }
    return "0x" + Buffer.from(str).toString("hex")
}
