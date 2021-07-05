/*
 *  ---license-start
 *  eu-digital-green-certificates / dgca-verifier-app-android
 *  ---
 *  Copyright (C) 2021 T-Systems International GmbH and all other contributors
 *  ---
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ---license-end
 *
 *  Created by osarapulov on 6/30/21 2:54 PM
 */

package dgca.verifier.app.engine.domain.rules

import dgca.verifier.app.engine.UTC_ZONE_ID
import dgca.verifier.app.engine.data.CertificateType
import dgca.verifier.app.engine.data.RuleCertificateType
import dgca.verifier.app.engine.data.Rule
import dgca.verifier.app.engine.data.Type
import dgca.verifier.app.engine.data.source.rules.RulesRepository
import java.time.ZonedDateTime
import java.util.*

/*-
 * ---license-start
 * eu-digital-green-certificates / dgc-certlogic-android
 * ---
 * Copyright (C) 2021 T-Systems International GmbH and all other contributors
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 *
 * Created by osarapulov on 30.06.21 14:54
 */
class DefaultGetRulesUseCase(private val rulesRepository: RulesRepository) : GetRulesUseCase {
    override fun invoke(
        acceptanceCountryIsoCode: String,
        issuanceCountryIsoCode: String,
        certificateType: CertificateType,
        region: String?
    ): List<Rule> {
        val acceptanceRules = mutableMapOf<RuleCertificateType, Rule>()
        rulesRepository.getRulesBy(
            acceptanceCountryIsoCode, ZonedDateTime.now().withZoneSameInstant(
                UTC_ZONE_ID
            ), Type.ACCEPTANCE, certificateType.toRuleCertificateType()
        ).forEach {
            if ((region.isNullOrEmpty() || region.toLowerCase(Locale.ROOT) == it.region) && (acceptanceRules[it.ruleCertificateType]?.version?.toVersion() ?: -1 < it.version.toVersion() ?: 0)) {
                acceptanceRules[it.ruleCertificateType] = it
            }
        }

        val invalidationRules = mutableMapOf<RuleCertificateType, Rule>()
        if (issuanceCountryIsoCode.isNotBlank()) {
            rulesRepository.getRulesBy(
                issuanceCountryIsoCode, ZonedDateTime.now().withZoneSameInstant(
                    UTC_ZONE_ID
                ), Type.INVALIDATION, certificateType.toRuleCertificateType()
            ).forEach {
                if (invalidationRules[it.ruleCertificateType]?.version?.toVersion() ?: -1 < it.version.toVersion() ?: 0) {
                    invalidationRules[it.ruleCertificateType] = it
                }
            }
        }
        return acceptanceRules.values + invalidationRules.values
    }

    private fun String.toVersion(): Int? = try {
        val versionParts = this.split('.')
        var version = 0
        var multiplier = 1
        versionParts.reversed().forEach {
            version += multiplier * it.toInt()
            multiplier *= 100
        }
        version
    } catch (error: Throwable) {
        null
    }
}