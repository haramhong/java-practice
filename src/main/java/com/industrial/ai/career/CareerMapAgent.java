package com.industrial.ai.career;

import java.util.*;

/**
 * CareerMapAgent – generates a career-change career map (転職キャリアマップ)
 * based on the skills demonstrated in the Industrial AI Demo pipeline.
 *
 * The agent:
 *   1. Registers the technical skills acquired while building the demo.
 *   2. Maps each skill to career roles that value that skill.
 *   3. Produces a structured career map report with recommended next steps.
 *
 * This demonstrates how the same AI/data-engineering skills used for industrial
 * IoT automation are directly transferable to cloud, AI/ML, and SRE roles.
 */
public class CareerMapAgent {

    /** A skill gained from building or operating the Industrial AI pipeline. */
    public record Skill(String name, String category, int level) {
        /** level: 1=beginner, 2=intermediate, 3=advanced */
    }

    /** A career role reachable with a given skill set. */
    public record CareerRole(String title, String description,
                             List<String> requiredSkills,
                             String transitionDifficulty) {}

    /** A personalised career recommendation. */
    public record CareerRecommendation(CareerRole role, int matchScore,
                                       List<String> matchedSkills,
                                       List<String> gapSkills) {}

    // -------------------------------------------------------------------------
    // Skill registry – built from running the Industrial AI Demo
    // -------------------------------------------------------------------------

    private final List<Skill> acquiredSkills = new ArrayList<>();

    public void registerDemoSkills() {
        acquiredSkills.addAll(List.of(
            new Skill("Java",               "Programming",   3),
            new Skill("OOP / Design",       "Programming",   3),
            new Skill("PLC / OT",           "Industrial",    2),
            new Skill("MQTT / IoT",         "Networking",    2),
            new Skill("Statistical AI",     "AI/ML",         2),
            new Skill("Anomaly Detection",  "AI/ML",         2),
            new Skill("Cloud Storage",      "Cloud",         2),
            new Skill("REST API / HTTP",    "Web",           2),
            new Skill("HTML / Dashboard",   "Web",           1),
            new Skill("Data Pipeline",      "Data Eng.",     2),
            new Skill("Maven / Build",      "DevOps",        2),
            new Skill("Unit Testing",       "Quality",       2)
        ));
    }

    /** Add a custom skill discovered outside the demo. */
    public void addSkill(Skill skill) {
        acquiredSkills.add(skill);
    }

    public List<Skill> getAcquiredSkills() {
        return Collections.unmodifiableList(acquiredSkills);
    }

    // -------------------------------------------------------------------------
    // Role catalogue
    // -------------------------------------------------------------------------

    private static final List<CareerRole> ROLE_CATALOGUE = List.of(
        new CareerRole(
            "IoT / OT-IT Engineer",
            "Bridges factory automation (OT) with enterprise IT systems.",
            List.of("Java", "MQTT / IoT", "PLC / OT", "Cloud Storage", "Data Pipeline"),
            "低 / Low"
        ),
        new CareerRole(
            "ML Engineer (Industrial AI)",
            "Builds and deploys machine-learning models for predictive maintenance.",
            List.of("Statistical AI", "Anomaly Detection", "Data Pipeline", "Cloud Storage", "Python"),
            "中 / Medium"
        ),
        new CareerRole(
            "Cloud / Backend Engineer",
            "Designs scalable back-end services and cloud data pipelines.",
            List.of("Java", "Cloud Storage", "REST API / HTTP", "Data Pipeline", "Maven / Build"),
            "低 / Low"
        ),
        new CareerRole(
            "Data Engineer",
            "Builds reliable data ingestion, transformation, and storage pipelines.",
            List.of("Data Pipeline", "Cloud Storage", "MQTT / IoT", "Java", "Statistical AI"),
            "低 / Low"
        ),
        new CareerRole(
            "Site Reliability Engineer (SRE)",
            "Ensures reliability and observability of production systems.",
            List.of("Maven / Build", "Unit Testing", "REST API / HTTP", "Data Pipeline", "Cloud Storage"),
            "中 / Medium"
        ),
        new CareerRole(
            "Full-Stack Web Developer",
            "Develops web applications from API to interactive front-end.",
            List.of("REST API / HTTP", "HTML / Dashboard", "Java", "OOP / Design", "Unit Testing"),
            "中 / Medium"
        ),
        new CareerRole(
            "AI/ML Researcher",
            "Researches and publishes novel machine-learning techniques.",
            List.of("Statistical AI", "Anomaly Detection", "Python", "Mathematics", "Research"),
            "高 / High"
        )
    );

    // -------------------------------------------------------------------------
    // Career map generation
    // -------------------------------------------------------------------------

    /**
     * Generate a ranked list of career recommendations based on acquired skills.
     *
     * @return recommendations sorted by match score descending
     */
    public List<CareerRecommendation> generateCareerMap() {
        Set<String> ownedSkillNames = new HashSet<>();
        for (Skill s : acquiredSkills) {
            ownedSkillNames.add(s.name());
        }

        List<CareerRecommendation> recommendations = new ArrayList<>();
        for (CareerRole role : ROLE_CATALOGUE) {
            List<String> matched = new ArrayList<>();
            List<String> gaps    = new ArrayList<>();
            for (String required : role.requiredSkills()) {
                if (ownedSkillNames.contains(required)) {
                    matched.add(required);
                } else {
                    gaps.add(required);
                }
            }
            int score = (int) Math.round(
                100.0 * matched.size() / role.requiredSkills().size());
            recommendations.add(new CareerRecommendation(role, score, matched, gaps));
        }

        recommendations.sort(Comparator.comparingInt(CareerRecommendation::matchScore).reversed());
        return recommendations;
    }

    /**
     * Render the career map as a formatted text report.
     *
     * @return multi-line report string
     */
    public String renderReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════════════════════════╗\n");
        sb.append("║  転職キャリアマップ / Career Transition Map               ║\n");
        sb.append("╚══════════════════════════════════════════════════════════╝\n\n");

        sb.append("■ 習得スキル / Acquired Skills\n");
        Map<String, List<Skill>> byCategory = new LinkedHashMap<>();
        for (Skill s : acquiredSkills) {
            byCategory.computeIfAbsent(s.category(), k -> new ArrayList<>()).add(s);
        }
        for (Map.Entry<String, List<Skill>> entry : byCategory.entrySet()) {
            sb.append("  [").append(entry.getKey()).append("]\n");
            for (Skill s : entry.getValue()) {
                sb.append("    ").append("★".repeat(s.level()))
                  .append(" ").append(s.name()).append("\n");
            }
        }

        sb.append("\n■ キャリア推薦 / Career Recommendations\n\n");
        List<CareerRecommendation> map = generateCareerMap();
        for (CareerRecommendation rec : map) {
            sb.append(String.format("  %-40s  適合度: %3d%%  難易度: %s%n",
                rec.role().title(), rec.matchScore(), rec.role().transitionDifficulty()));
            if (!rec.gapSkills().isEmpty()) {
                sb.append("    → 不足スキル / Skill gaps: ")
                  .append(String.join(", ", rec.gapSkills())).append("\n");
            } else {
                sb.append("    → ✅ スキルセット完全一致！ Perfect skill match!\n");
            }
            sb.append("\n");
        }

        sb.append("■ 次のアクション / Next Actions\n");
        sb.append("  1. 最適合のロールに向けたスキルギャップを埋める。\n");
        sb.append("     Fill the skill gaps for your best-matched role.\n");
        sb.append("  2. GitHubポートフォリオにこのデモを掲載する。\n");
        sb.append("     Showcase this demo in your GitHub portfolio.\n");
        sb.append("  3. IoT / AI / クラウド関連の求人に応募する。\n");
        sb.append("     Apply to IoT / AI / Cloud engineering positions.\n");

        return sb.toString();
    }
}
