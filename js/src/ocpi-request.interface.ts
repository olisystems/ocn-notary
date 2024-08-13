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
 * All possible OCPI request/response headers used in OCPI v2.2
 */
export interface ISignableHeaders {
    "x-correlation-id"?: string
    "ocpi-to-country-code"?: string
    "ocpi-to-party-id"?: string
    "ocpi-from-country-code"?: string
    "ocpi-from-party-id"?: string
    "x-limit"?: string
    "x-total-count"?: string
    "link"?: string
    "location"?: string
}

/**
 * Ocpi Request Variables which should be signed/verified by the Notary
 */
export interface IValuesToSign {
    headers: ISignableHeaders
    params?: { [key: string]: any }
    body?: any
}
