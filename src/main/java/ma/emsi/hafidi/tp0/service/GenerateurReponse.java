package ma.emsi.hafidi.tp0.service;

import jakarta.enterprise.context.Dependent;

import java.io.Serializable;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Classe de service pour les modificateurs de question.
 * Un modificateur de question prend en entrée une question et retourne
 * la question modifiée.
 *
 * Exercice bonus : la réponse est une petite fiche d'analyse du message de l'utilisateur
 * (type de phrase, langue probable, statistiques, mot le plus long, phrase "miroir").
 */
@Dependent // Portée CDI pour rendre la classe injectable par CDI dans les autres classes.
// Portée Dependent : l'instance sera supprimée quand la classe qui l'a injectée sera supprimée.
public class GenerateurReponse implements Serializable { // Serializable car CDI peut mettre l'instance en mémoire secondaire.
    private boolean roleSystemeAffiche = false;

    /**
     * Nombre de messages analysés depuis le début du chat.
     * Comme l'instance dépend du backing bean (portée view), le compteur repart à 0 à chaque nouveau chat.
     */
    private int nombreMessages = 0;

    /**
     * Petits mots très fréquents qui servent à deviner la langue du message.
     */
    private static final Set<String> MOTS_FRANCAIS = Set.of(
            "le", "la", "les", "un", "une", "des", "de", "du", "et", "est", "je", "tu", "il", "elle",
            "nous", "vous", "que", "qui", "quoi", "pour", "dans", "avec", "pas", "ce", "quel", "quelle",
            "comment", "pourquoi", "où", "mon", "ma", "mes", "sur", "au", "aux");
    private static final Set<String> MOTS_ANGLAIS = Set.of(
            "the", "a", "an", "and", "is", "are", "i", "you", "he", "she", "we", "they", "what", "who",
            "for", "in", "with", "not", "this", "that", "how", "why", "where", "my", "on", "to", "of", "do");

    /**
     * Génère une réponse à une question : une fiche d'analyse du message.
     * @param question La question à analyser.
     * @param roleSysteme Le rôle système ; il est rappelé au début de la première réponse du chat.
     * @return La fiche d'analyse de la question.
     */
    public String genererReponse(String question, String roleSysteme) {
        nombreMessages++;
        StringBuilder resultat = new StringBuilder();
        if (!this.roleSystemeAffiche && roleSysteme != null) {
            // Rappel du rôle choisi (sa première ligne) au début du chat.
            resultat.append("Rôle : ").append(roleSysteme.strip().lines().findFirst().orElse("")).append("\n\n");
            this.roleSystemeAffiche = true;
        }

        String texte = question.strip();
        List<String> mots = extraireMots(texte);

        resultat.append("Analyse du message n°").append(nombreMessages).append("\n");
        resultat.append("- Type : ").append(typeDePhrase(texte)).append("\n");
        resultat.append("- Langue probable : ").append(langueProbable(mots)).append("\n");
        resultat.append("- Mots : ").append(mots.size())
                .append(", lettres : ").append(compterLettres(texte))
                .append(", voyelles : ").append(compterVoyelles(texte)).append("\n");
        resultat.append("- Mot le plus long : ").append(motLePlusLong(mots)).append("\n");
        resultat.append("- Miroir : ").append(miroir(texte));
        return resultat.toString();
    }

    /**
     * Découpe le texte en mots (lettres, chiffres et apostrophes internes), en minuscules.
     */
    private List<String> extraireMots(String texte) {
        List<String> mots = new ArrayList<>();
        for (String mot : texte.toLowerCase(Locale.FRENCH).split("[^\\p{L}\\p{N}']+")) {
            // "l'eau" donne "eau" : on garde la partie après l'apostrophe.
            String motNettoye = mot.contains("'") ? mot.substring(mot.lastIndexOf('\'') + 1) : mot;
            if (!motNettoye.isEmpty()) {
                mots.add(motNettoye);
            }
        }
        return mots;
    }

    private String typeDePhrase(String texte) {
        if (texte.endsWith("?")) {
            return "question";
        }
        if (texte.endsWith("!")) {
            return "exclamation";
        }
        return "affirmation";
    }

    /**
     * Compte les petits mots français et anglais du message pour deviner sa langue.
     */
    private String langueProbable(List<String> mots) {
        long scoreFrancais = mots.stream().filter(MOTS_FRANCAIS::contains).count();
        long scoreAnglais = mots.stream().filter(MOTS_ANGLAIS::contains).count();
        if (scoreFrancais > scoreAnglais) {
            return "français";
        }
        if (scoreAnglais > scoreFrancais) {
            return "anglais";
        }
        return "indéterminée";
    }

    private long compterLettres(String texte) {
        return texte.codePoints().filter(Character::isLetter).count();
    }

    /**
     * Compte les voyelles, y compris les voyelles accentuées (é, à, ô...) :
     * les accents sont d'abord séparés des lettres puis supprimés.
     */
    private long compterVoyelles(String texte) {
        String sansAccents = Normalizer.normalize(texte.toLowerCase(Locale.FRENCH), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sansAccents.chars().filter(c -> "aeiouy".indexOf(c) >= 0).count();
    }

    private String motLePlusLong(List<String> mots) {
        String plusLong = "";
        for (String mot : mots) {
            if (mot.length() > plusLong.length()) {
                plusLong = mot;
            }
        }
        return plusLong.isEmpty() ? "(aucun)" : plusLong;
    }

    /**
     * La phrase avec ses mots dans l'ordre inverse (la ponctuation finale est retirée).
     */
    private String miroir(String texte) {
        List<String> mots = new ArrayList<>(List.of(texte.replaceAll("[\\s?!.]+$", "").split("\\s+")));
        Collections.reverse(mots);
        return String.join(" ", mots);
    }
}
