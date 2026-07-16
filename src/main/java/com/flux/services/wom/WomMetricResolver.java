package com.flux.services.wom;

import static net.runelite.client.hiscore.HiscoreSkill.CHAMBERS_OF_XERIC_CHALLENGE_MODE;
import static net.runelite.client.hiscore.HiscoreSkill.KREEARRA;
import static net.runelite.client.hiscore.HiscoreSkill.KRIL_TSUTSAROTH;
import static net.runelite.client.hiscore.HiscoreSkill.PHOSANIS_NIGHTMARE;
import static net.runelite.client.hiscore.HiscoreSkill.RUNECRAFT;
import static net.runelite.client.hiscore.HiscoreSkill.THEATRE_OF_BLOOD_HARD_MODE;
import static net.runelite.client.hiscore.HiscoreSkill.TOMBS_OF_AMASCUT_EXPERT;
import static net.runelite.client.hiscore.HiscoreSkill.TZKAL_ZUK;
import static net.runelite.client.hiscore.HiscoreSkill.TZTOK_JAD;
import static net.runelite.client.hiscore.HiscoreSkill.VETION;

import java.util.Map;
import java.util.Optional;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.HiscoreSkillType;

/**
 * Resolves raw Wise Old Man competition "metric" strings to the HiscoreSkill enum constant used for
 * icon/sprite lookups, for both SOTW skills and BOTM bosses.
 */
public final class WomMetricResolver {

	private static final Map<String, HiscoreSkill> SKILL_METRIC_OVERRIDES = Map.of(
		"runecrafting", RUNECRAFT
	);

	private static final Map<String, HiscoreSkill> BOSS_METRIC_OVERRIDES = Map.of(
		"kreearra", KREEARRA,
		"kril_tsutsaroth", KRIL_TSUTSAROTH,
		"phosanis_nightmare", PHOSANIS_NIGHTMARE,
		"vetion", VETION,
		"tzkal_zuk", TZKAL_ZUK,
		"tztok_jad", TZTOK_JAD,
		"theatre_of_blood_hard_mode", THEATRE_OF_BLOOD_HARD_MODE,
		"tombs_of_amascut_expert", TOMBS_OF_AMASCUT_EXPERT,
		"chambers_of_xeric_challenge_mode", CHAMBERS_OF_XERIC_CHALLENGE_MODE
	);

	private WomMetricResolver() {
	}

	public static Optional<HiscoreSkill> resolveSkill(String womMetric) {
		return resolve(womMetric, SKILL_METRIC_OVERRIDES, HiscoreSkillType.SKILL);
	}

	public static Optional<HiscoreSkill> resolveBoss(String womMetric) {
		return resolve(womMetric, BOSS_METRIC_OVERRIDES, HiscoreSkillType.BOSS);
	}

	private static Optional<HiscoreSkill> resolve(String womMetric,
		Map<String, HiscoreSkill> overrides, HiscoreSkillType type) {
		if (womMetric == null || womMetric.isEmpty()) {
			return Optional.empty();
		}

		if (overrides.containsKey(womMetric)) {
			return Optional.of(overrides.get(womMetric));
		}

		String normalized = womMetric.replace('_', ' ');
		for (HiscoreSkill skill : HiscoreSkill.values()) {
			if (skill.getType() == type && skill.getName().equalsIgnoreCase(normalized)) {
				return Optional.of(skill);
			}
		}

		return Optional.empty();
	}
}
