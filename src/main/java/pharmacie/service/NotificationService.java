package pharmacie.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pharmacie.dao.FournisseurRepository;
import pharmacie.dao.MedicamentRepository;
import pharmacie.entity.Fournisseur;
import pharmacie.entity.Medicament;

import java.util.List;

/**
 * Service de notification pour le réapprovisionnement des médicaments.
 * Lorsqu'un médicament atteint son niveau de réapprovisionnement,
 * ce service envoie un email aux fournisseurs concernés via SendGrid.
 */
@Slf4j
@Service
public class NotificationService {

    private final MedicamentRepository medicamentDao;
    private final FournisseurRepository fournisseurDao;
    private final EmailService emailService;

    public NotificationService(MedicamentRepository medicamentDao,
                               FournisseurRepository fournisseurDao,
                               EmailService emailService) {
        this.medicamentDao = medicamentDao;
        this.fournisseurDao = fournisseurDao;
        this.emailService = emailService;
    }

    /**
     * Vérifie si un médicament a atteint son niveau de réapprovisionnement
     * et notifie les fournisseurs associés à sa catégorie par email.
     *
     * @param medicament le médicament à vérifier
     */
    public void verifierEtNotifierReappro(Medicament medicament) {
        if (medicament.isIndisponible()) {
            return; // Pas de notification pour les médicaments indisponibles
        }

        if (medicament.getUnitesEnStock() <= medicament.getNiveauDeReappro()) {
            log.warn("⚠️ Médicament '{}' a atteint le niveau de réapprovisionnement. " +
                    "Stock: {}, Niveau de réappro: {}",
                    medicament.getNom(),
                    medicament.getUnitesEnStock(),
                    medicament.getNiveauDeReappro());

            notifierFournisseurs(medicament);
        }
    }

    /**
     * Notifie tous les fournisseurs associés à la catégorie du médicament
     * qui a atteint son niveau de réapprovisionnement.
     *
     * @param medicament le médicament nécessitant un réapprovisionnement
     */
    private void notifierFournisseurs(Medicament medicament) {
        Integer categorieCode = medicament.getCategorie().getCode();
        List<Fournisseur> fournisseurs = fournisseurDao.findByCategorieCode(categorieCode);

        if (fournisseurs.isEmpty()) {
            log.warn("Aucun fournisseur trouvé pour la catégorie '{}' du médicament '{}'",
                    medicament.getCategorie().getLibelle(), medicament.getNom());
            return;
        }

        String subject = "🔔 Alerte réapprovisionnement - " + medicament.getNom();
        String body = buildEmailBody(medicament);

        for (Fournisseur fournisseur : fournisseurs) {
            log.info("Envoi d'une notification de réappro à {} ({}) pour le médicament '{}'",
                    fournisseur.getNom(), fournisseur.getEmail(), medicament.getNom());
            emailService.sendEmail(fournisseur.getEmail(), subject, body);
        }
    }

    /**
     * Construit le corps HTML de l'email de notification de réapprovisionnement
     *
     * @param medicament le médicament concerné
     * @return le HTML de l'email
     */
    private String buildEmailBody(Medicament medicament) {
        return """
            <html>
            <body style="font-family: Arial, sans-serif; padding: 20px;">
                <h2 style="color: #d9534f;">🔔 Alerte de Réapprovisionnement</h2>
                <p>Bonjour,</p>
                <p>Le médicament suivant a atteint son niveau de réapprovisionnement et nécessite une nouvelle livraison :</p>
                <table style="border-collapse: collapse; width: 100%%; max-width: 500px;">
                    <tr style="background-color: #f5f5f5;">
                        <td style="padding: 8px; border: 1px solid #ddd;"><strong>Médicament</strong></td>
                        <td style="padding: 8px; border: 1px solid #ddd;">%s</td>
                    </tr>
                    <tr>
                        <td style="padding: 8px; border: 1px solid #ddd;"><strong>Catégorie</strong></td>
                        <td style="padding: 8px; border: 1px solid #ddd;">%s</td>
                    </tr>
                    <tr style="background-color: #f5f5f5;">
                        <td style="padding: 8px; border: 1px solid #ddd;"><strong>Stock actuel</strong></td>
                        <td style="padding: 8px; border: 1px solid #ddd; color: #d9534f;">%d unités</td>
                    </tr>
                    <tr>
                        <td style="padding: 8px; border: 1px solid #ddd;"><strong>Niveau de réappro</strong></td>
                        <td style="padding: 8px; border: 1px solid #ddd;">%d unités</td>
                    </tr>
                    <tr style="background-color: #f5f5f5;">
                        <td style="padding: 8px; border: 1px solid #ddd;"><strong>Unités commandées</strong></td>
                        <td style="padding: 8px; border: 1px solid #ddd;">%d unités</td>
                    </tr>
                    <tr>
                        <td style="padding: 8px; border: 1px solid #ddd;"><strong>Prix unitaire</strong></td>
                        <td style="padding: 8px; border: 1px solid #ddd;">%s €</td>
                    </tr>
                </table>
                <p>Merci de prendre les dispositions nécessaires pour le réapprovisionnement.</p>
                <p>Cordialement,<br><strong>Pharmacie Centrale</strong></p>
            </body>
            </html>
            """.formatted(
                medicament.getNom(),
                medicament.getCategorie().getLibelle(),
                medicament.getUnitesEnStock(),
                medicament.getNiveauDeReappro(),
                medicament.getUnitesCommandees(),
                medicament.getPrixUnitaire().toString()
        );
    }

    /**
     * Vérifie tous les médicaments et envoie des notifications pour ceux
     * qui ont atteint leur niveau de réapprovisionnement.
     * Peut être appelé par un scheduler ou manuellement via l'API.
     *
     * @return le nombre de médicaments nécessitant un réapprovisionnement
     */
    @Transactional(readOnly = true)
    public int verifierTousLesMedicaments() {
        log.info("Vérification du stock de tous les médicaments...");
        List<Medicament> tousLesMedicaments = medicamentDao.findAll();
        int count = 0;
        for (Medicament medicament : tousLesMedicaments) {
            if (!medicament.isIndisponible() &&
                medicament.getUnitesEnStock() <= medicament.getNiveauDeReappro()) {
                notifierFournisseurs(medicament);
                count++;
            }
        }
        log.info("{} médicament(s) nécessite(nt) un réapprovisionnement", count);
        return count;
    }
}
