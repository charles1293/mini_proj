package pharmacie.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import pharmacie.dao.CategorieRepository;
import pharmacie.dao.FournisseurRepository;
import pharmacie.entity.Categorie;
import pharmacie.entity.Fournisseur;

@Slf4j
@Service
public class FournisseurService {

    private final FournisseurRepository fournisseurDao;
    private final CategorieRepository categorieDao;

    public FournisseurService(FournisseurRepository fournisseurDao, CategorieRepository categorieDao) {
        this.fournisseurDao = fournisseurDao;
        this.categorieDao = categorieDao;
    }

    /**
     * Récupère tous les fournisseurs
     * @return la liste de tous les fournisseurs
     */
    @Transactional(readOnly = true)
    public List<Fournisseur> findAll() {
        return fournisseurDao.findAll();
    }

    /**
     * Récupère un fournisseur par son id
     * @param id l'identifiant du fournisseur
     * @return le fournisseur correspondant
     * @throws NoSuchElementException si le fournisseur n'existe pas
     */
    @Transactional(readOnly = true)
    public Fournisseur findById(Long id) {
        return fournisseurDao.findById(id).orElseThrow(
            () -> new NoSuchElementException("Fournisseur avec l'id " + id + " introuvable")
        );
    }

    /**
     * Recherche les fournisseurs dont le nom contient une sous-chaîne
     * @param nom la sous-chaîne à rechercher
     * @return la liste des fournisseurs correspondants
     */
    @Transactional(readOnly = true)
    public List<Fournisseur> findByNomContaining(String nom) {
        return fournisseurDao.findByNomContainingIgnoreCase(nom);
    }

    /**
     * Recherche les fournisseurs qui fournissent une catégorie donnée
     * @param categorieCode le code de la catégorie
     * @return la liste des fournisseurs pour cette catégorie
     */
    @Transactional(readOnly = true)
    public List<Fournisseur> findByCategorieCode(Integer categorieCode) {
        return fournisseurDao.findByCategorieCode(categorieCode);
    }

    /**
     * Crée un nouveau fournisseur
     * @param nom le nom du fournisseur
     * @param email l'email du fournisseur
     * @return le fournisseur créé
     * @throws DataIntegrityViolationException si le nom existe déjà
     */
    @Transactional
    public Fournisseur creerFournisseur(String nom, String email) {
        log.info("Service : Création du fournisseur '{}'", nom);
        var fournisseur = new Fournisseur(nom, email);
        return fournisseurDao.save(fournisseur);
    }

    /**
     * Met à jour un fournisseur existant
     * @param id l'identifiant du fournisseur
     * @param nom le nouveau nom (optionnel)
     * @param email le nouvel email (optionnel)
     * @return le fournisseur mis à jour
     * @throws NoSuchElementException si le fournisseur n'existe pas
     */
    @Transactional
    public Fournisseur updateFournisseur(Long id, String nom, String email) {
        log.info("Service : Mise à jour du fournisseur {}", id);
        var fournisseur = fournisseurDao.findById(id).orElseThrow(
            () -> new NoSuchElementException("Fournisseur avec l'id " + id + " introuvable")
        );
        if (nom != null && !nom.isBlank()) {
            fournisseur.setNom(nom);
        }
        if (email != null && !email.isBlank()) {
            fournisseur.setEmail(email);
        }
        return fournisseurDao.save(fournisseur);
    }

    /**
     * Supprime un fournisseur
     * @param id l'identifiant du fournisseur
     * @throws NoSuchElementException si le fournisseur n'existe pas
     */
    @Transactional
    public void deleteFournisseur(Long id) {
        log.info("Service : Suppression du fournisseur {}", id);
        var fournisseur = fournisseurDao.findById(id).orElseThrow(
            () -> new NoSuchElementException("Fournisseur avec l'id " + id + " introuvable")
        );
        // Retirer le fournisseur de toutes les catégories associées
        for (Categorie categorie : fournisseur.getCategories()) {
            categorie.getFournisseurs().remove(fournisseur);
        }
        fournisseur.getCategories().clear();
        fournisseurDao.delete(fournisseur);
    }

    /**
     * Associe un fournisseur à une catégorie
     * @param fournisseurId l'identifiant du fournisseur
     * @param categorieCode le code de la catégorie
     * @return le fournisseur mis à jour
     * @throws NoSuchElementException si le fournisseur ou la catégorie n'existe pas
     * @throws IllegalStateException si l'association existe déjà
     */
    @Transactional
    public Fournisseur ajouterCategorie(Long fournisseurId, Integer categorieCode) {
        log.info("Service : Association du fournisseur {} à la catégorie {}", fournisseurId, categorieCode);
        var fournisseur = fournisseurDao.findById(fournisseurId).orElseThrow(
            () -> new NoSuchElementException("Fournisseur avec l'id " + fournisseurId + " introuvable")
        );
        var categorie = categorieDao.findById(categorieCode).orElseThrow(
            () -> new NoSuchElementException("Catégorie avec le code " + categorieCode + " introuvable")
        );
        // Vérifier que l'association n'existe pas déjà
        if (categorie.getFournisseurs().contains(fournisseur)) {
            throw new IllegalStateException(
                "Le fournisseur '" + fournisseur.getNom() + "' est déjà associé à la catégorie '" + categorie.getLibelle() + "'"
            );
        }
        categorie.getFournisseurs().add(fournisseur);
        fournisseur.getCategories().add(categorie);
        return fournisseurDao.save(fournisseur);
    }

    /**
     * Retire l'association d'un fournisseur à une catégorie
     * @param fournisseurId l'identifiant du fournisseur
     * @param categorieCode le code de la catégorie
     * @return le fournisseur mis à jour
     * @throws NoSuchElementException si le fournisseur ou la catégorie n'existe pas
     */
    @Transactional
    public Fournisseur retirerCategorie(Long fournisseurId, Integer categorieCode) {
        log.info("Service : Retrait de l'association du fournisseur {} à la catégorie {}", fournisseurId, categorieCode);
        var fournisseur = fournisseurDao.findById(fournisseurId).orElseThrow(
            () -> new NoSuchElementException("Fournisseur avec l'id " + fournisseurId + " introuvable")
        );
        var categorie = categorieDao.findById(categorieCode).orElseThrow(
            () -> new NoSuchElementException("Catégorie avec le code " + categorieCode + " introuvable")
        );
        categorie.getFournisseurs().remove(fournisseur);
        fournisseur.getCategories().remove(categorie);
        return fournisseurDao.save(fournisseur);
    }
}
