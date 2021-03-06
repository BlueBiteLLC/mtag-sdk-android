//Copyright 2018 Blue Bite LLC.
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//

package com.bluebite.mtag_sdk;


import org.json.JSONObject;

/**
* Implementation which will be called by the SDK upon success/failure to authenticate a tag.
*/
public interface BlueBiteInteractionDelegate {
    /**
     * Called when an Interaction has been successfully registered with the BlueBite API.
     * @param results Abbreviated payload returned by the authentication API.
     */
    void interactionDataWasReceived(JSONObject results);

    /**
     * Called when an Interaction has failed to be registered with the BlueBite API.
     * This could be caused by a general API failure, an invalid URL, or a general Volley failure.
     * @param error Basic description of what went wrong.
     */
    void interactionDidFail(String error);
}
