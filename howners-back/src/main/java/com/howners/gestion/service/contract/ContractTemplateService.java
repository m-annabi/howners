package com.howners.gestion.service.contract;

import com.howners.gestion.domain.contract.ContractTemplate;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.rental.RentalType;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.template.*;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.ContractTemplateRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.util.UserDisplayUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ContractTemplateService {

    private final ContractTemplateRepository contractTemplateRepository;
    private final RentalRepository rentalRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Remplit un template avec les données d'une location
     */
    public String fillTemplate(ContractTemplate template, Rental rental) {
        Map<String, String> variables = buildVariablesMap(rental);
        return replaceVariables(template.getContent(), variables);
    }

    /**
     * Récupère le template par défaut pour un type de location
     */
    public ContractTemplate getDefaultTemplate(RentalType rentalType) {
        return contractTemplateRepository.findByRentalTypeAndIsDefaultTrue(rentalType)
                .orElseThrow(() -> new ResourceNotFoundException("Default template for type " + rentalType, "rentalType", rentalType.toString()));
    }

    /**
     * Construit la map des variables à partir d'une location
     */
    private Map<String, String> buildVariablesMap(Rental rental) {
        Map<String, String> variables = new HashMap<>();

        Property property = rental.getProperty();
        User owner = property.getOwner();
        User tenant = rental.getTenant();

        // Variables propriétaire
        variables.put("owner.firstName", owner.getFirstName() != null ? owner.getFirstName() : "");
        variables.put("owner.lastName", owner.getLastName() != null ? owner.getLastName() : "");
        variables.put("owner.fullName", UserDisplayUtils.getFullName(owner));
        variables.put("owner.email", owner.getEmail());
        variables.put("owner.phone", owner.getPhone() != null ? owner.getPhone() : "");

        // Variables locataire (gérer le cas où tenant est null)
        if (tenant != null) {
            variables.put("tenant.firstName", tenant.getFirstName() != null ? tenant.getFirstName() : "");
            variables.put("tenant.lastName", tenant.getLastName() != null ? tenant.getLastName() : "");
            variables.put("tenant.fullName", UserDisplayUtils.getFullName(tenant));
            variables.put("tenant.email", tenant.getEmail());
            variables.put("tenant.phone", tenant.getPhone() != null ? tenant.getPhone() : "");
        } else {
            // Valeurs par défaut si pas de locataire assigné
            variables.put("tenant.firstName", "[Prénom Locataire]");
            variables.put("tenant.lastName", "[Nom Locataire]");
            variables.put("tenant.fullName", "[Nom Complet Locataire]");
            variables.put("tenant.email", "[Email Locataire]");
            variables.put("tenant.phone", "[Téléphone Locataire]");
        }

        // Variables propriété
        variables.put("property.name", property.getName());
        variables.put("property.type", property.getPropertyType().name());
        variables.put("property.address.street", property.getAddressLine1() != null ? property.getAddressLine1() : "");
        variables.put("property.address.city", property.getCity() != null ? property.getCity() : "");
        variables.put("property.address.postalCode", property.getPostalCode() != null ? property.getPostalCode() : "");
        variables.put("property.address.country", property.getCountry() != null ? property.getCountry() : "");
        variables.put("property.address.full", getFullAddress(property));
        variables.put("property.surface", property.getSurfaceArea() != null ? property.getSurfaceArea().toString() : "");
        variables.put("property.rooms", "");  // Not available in Property entity
        variables.put("property.bedrooms", property.getBedrooms() != null ? property.getBedrooms().toString() : "");

        // Variables location
        variables.put("rental.startDate", formatDate(rental.getStartDate()));
        variables.put("rental.endDate", rental.getEndDate() != null ? formatDate(rental.getEndDate()) : "Indéterminée");
        variables.put("rental.monthlyRent", formatAmount(rental.getMonthlyRent()));
        variables.put("rental.depositAmount", rental.getDepositAmount() != null ? formatAmount(rental.getDepositAmount()) : "0");
        variables.put("rental.type", rental.getRentalType().name());
        variables.put("rental.status", rental.getStatus().name());

        // Variables date du jour
        variables.put("today", formatDate(LocalDate.now()));

        log.debug("Built {} variables for rental {}", variables.size(), rental.getId());
        return variables;
    }

    /**
     * Remplace toutes les variables {{variable}} dans le contenu
     */
    private String replaceVariables(String content, Map<String, String> variables) {
        // Nettoyer le HTML de Quill pour que les variables soient correctement détectées
        String result = cleanVariablesInHtml(content);

        log.debug("Variables disponibles: {}", variables.keySet());
        log.debug("Contenu après nettoyage (premiers 500 chars): {}",
                result.length() > 500 ? result.substring(0, 500) : result);

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            if (result.contains(placeholder)) {
                log.info("Remplacement variable: {} -> {}", placeholder, entry.getValue());
            }
            result = result.replace(placeholder, entry.getValue());
        }

        // Vérifier s'il reste des variables non remplacées
        Pattern leftover = Pattern.compile("\\{\\{(.*?)\\}\\}");
        Matcher leftoverMatcher = leftover.matcher(result);
        while (leftoverMatcher.find()) {
            log.warn("Variable non remplacée: {{}}", leftoverMatcher.group(1));
        }

        return result;
    }

    /**
     * Nettoie les balises HTML et caractères invisibles à l'intérieur des placeholders {{ }}
     * pour que le remplacement de variables fonctionne avec le contenu HTML de Quill
     */
    private String cleanVariablesInHtml(String htmlContent) {
        if (htmlContent == null) return "";

        // 1. Supprimer TOUS les caractères zero-width de tout le contenu
        //    (Quill insère des zero-width spaces autour du curseur)
        String result = htmlContent
                .replaceAll("[\\u200B\\u200C\\u200D\\uFEFF\\u00AD]", "");

        // 2. Remplacer les entités HTML pour les accolades
        result = result
                .replace("&#123;", "{")
                .replace("&#125;", "}")
                .replace("&#x7B;", "{")
                .replace("&#x7D;", "}")
                .replace("&lbrace;", "{")
                .replace("&rbrace;", "}");

        // 3. Réunifier les {{ }} qui pourraient être séparés par des tags HTML
        //    Ex: {<span>{</span> → {{  ou  }<span>}</span> → }}
        result = result.replaceAll("\\}(\\s*</?(span|strong|em|b|i|u|a|p|br|div|font|sub|sup)(\\s[^>]*)?>\\s*)*\\}", "}}");
        result = result.replaceAll("\\{(\\s*</?(span|strong|em|b|i|u|a|p|br|div|font|sub|sup)(\\s[^>]*)?>\\s*)*\\{", "{{");

        // 4. Nettoyer les tags HTML DANS les {{ }}
        Pattern pattern = Pattern.compile("\\{\\{(.*?)\\}\\}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(result);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String variableContent = matcher.group(1);
            // Supprimer les balises HTML, &nbsp;, espaces superflus
            String cleanVariable = variableContent
                    .replaceAll("<[^>]*>", "")
                    .replace("&nbsp;", "")
                    .trim();
            log.debug("Variable trouvée dans HTML: '{}' -> nettoyée: '{}'", variableContent, cleanVariable);
            matcher.appendReplacement(sb, Matcher.quoteReplacement("{{" + cleanVariable + "}}"));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String getFullAddress(Property property) {
        return String.format("%s, %s %s, %s",
                property.getAddressLine1() != null ? property.getAddressLine1() : "",
                property.getPostalCode() != null ? property.getPostalCode() : "",
                property.getCity() != null ? property.getCity() : "",
                property.getCountry() != null ? property.getCountry() : "");
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }

    private String formatAmount(BigDecimal amount) {
        return amount != null ? String.format("%.2f €", amount) : "0.00 €";
    }

    /**
     * Récupère toutes les variables disponibles avec leurs descriptions
     */
    public TemplateVariablesResponse getAvailableVariables() {
        List<TemplateVariablesResponse.VariableInfo> variables = new ArrayList<>();

        // Variables propriétaire
        variables.add(new TemplateVariablesResponse.VariableInfo("owner.firstName", "Prénom du propriétaire", "owner", "Jean"));
        variables.add(new TemplateVariablesResponse.VariableInfo("owner.lastName", "Nom du propriétaire", "owner", "Dupont"));
        variables.add(new TemplateVariablesResponse.VariableInfo("owner.fullName", "Nom complet du propriétaire", "owner", "Jean Dupont"));
        variables.add(new TemplateVariablesResponse.VariableInfo("owner.email", "Email du propriétaire", "owner", "jean.dupont@email.com"));
        variables.add(new TemplateVariablesResponse.VariableInfo("owner.phone", "Téléphone du propriétaire", "owner", "+33 6 12 34 56 78"));

        // Variables locataire
        variables.add(new TemplateVariablesResponse.VariableInfo("tenant.firstName", "Prénom du locataire", "tenant", "Marie"));
        variables.add(new TemplateVariablesResponse.VariableInfo("tenant.lastName", "Nom du locataire", "tenant", "Martin"));
        variables.add(new TemplateVariablesResponse.VariableInfo("tenant.fullName", "Nom complet du locataire", "tenant", "Marie Martin"));
        variables.add(new TemplateVariablesResponse.VariableInfo("tenant.email", "Email du locataire", "tenant", "marie.martin@email.com"));
        variables.add(new TemplateVariablesResponse.VariableInfo("tenant.phone", "Téléphone du locataire", "tenant", "+33 6 98 76 54 32"));

        // Variables propriété
        variables.add(new TemplateVariablesResponse.VariableInfo("property.name", "Nom de la propriété", "property", "Appartement 2 pièces Centre-ville"));
        variables.add(new TemplateVariablesResponse.VariableInfo("property.type", "Type de propriété", "property", "APARTMENT"));
        variables.add(new TemplateVariablesResponse.VariableInfo("property.address.street", "Adresse (rue)", "property", "15 Rue de la République"));
        variables.add(new TemplateVariablesResponse.VariableInfo("property.address.city", "Ville", "property", "Paris"));
        variables.add(new TemplateVariablesResponse.VariableInfo("property.address.postalCode", "Code postal", "property", "75001"));
        variables.add(new TemplateVariablesResponse.VariableInfo("property.address.country", "Pays", "property", "France"));
        variables.add(new TemplateVariablesResponse.VariableInfo("property.address.full", "Adresse complète", "property", "15 Rue de la République, 75001 Paris, France"));
        variables.add(new TemplateVariablesResponse.VariableInfo("property.surface", "Surface (m²)", "property", "45"));
        variables.add(new TemplateVariablesResponse.VariableInfo("property.bedrooms", "Nombre de chambres", "property", "2"));

        // Variables location
        variables.add(new TemplateVariablesResponse.VariableInfo("rental.startDate", "Date de début", "rental", "01/01/2025"));
        variables.add(new TemplateVariablesResponse.VariableInfo("rental.endDate", "Date de fin", "rental", "31/12/2025"));
        variables.add(new TemplateVariablesResponse.VariableInfo("rental.monthlyRent", "Loyer mensuel", "rental", "850.00 €"));
        variables.add(new TemplateVariablesResponse.VariableInfo("rental.depositAmount", "Montant du dépôt de garantie", "rental", "1700.00 €"));
        variables.add(new TemplateVariablesResponse.VariableInfo("rental.type", "Type de location", "rental", "LONG_TERM"));
        variables.add(new TemplateVariablesResponse.VariableInfo("rental.status", "Statut de la location", "rental", "ACTIVE"));

        // Variables date
        variables.add(new TemplateVariablesResponse.VariableInfo("today", "Date du jour", "date", LocalDate.now().format(DATE_FORMATTER)));

        return new TemplateVariablesResponse(variables);
    }

    /**
     * Prévisualise un template rempli avec les données d'une location
     */
    public PreviewTemplateResponse previewTemplate(UUID templateId, UUID rentalId) {
        ContractTemplate template = contractTemplateRepository.findByIdWithCreatedBy(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template", "id", "unknown"));

        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental", "id", "unknown"));

        String filledContent = fillTemplate(template, rental);

        return new PreviewTemplateResponse(
                filledContent,
                rental.getProperty().getName(),
                UserDisplayUtils.getFullName(rental.getTenant())
        );
    }

    /**
     * Prévisualise un contenu personnalisé rempli avec les données d'une location
     */
    public PreviewTemplateResponse previewCustomContent(String customContent, UUID rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental", "id", "unknown"));

        Map<String, String> variables = buildVariablesMap(rental);
        String filledContent = replaceVariables(customContent, variables);

        return new PreviewTemplateResponse(
                filledContent,
                rental.getProperty().getName(),
                UserDisplayUtils.getFullName(rental.getTenant())
        );
    }

    /**
     * Remplit un contenu personnalisé avec les variables d'une location
     * Utilisé pour la modification de contrats
     */
    public String fillCustomContent(String customContent, Rental rental) {
        log.info("fillCustomContent appelé - contenu length={}, rental={}, tenant={}",
                customContent != null ? customContent.length() : 0,
                rental.getId(),
                rental.getTenant() != null ? rental.getTenant().getEmail() : "NULL");
        Map<String, String> variables = buildVariablesMap(rental);
        return replaceVariables(customContent, variables);
    }

    /**
     * Crée un nouveau template personnalisé
     */
    @Transactional
    public ContractTemplate createTemplate(CreateTemplateRequest request, User currentUser) {
        log.info("Creating new template: {} by user: {}", request.name(), currentUser.getId());

        ContractTemplate template = ContractTemplate.builder()
                .name(request.name())
                .description(request.description())
                .rentalType(request.rentalType())
                .content(request.content())
                .isDefault(false)
                .isActive(true)
                .createdBy(currentUser)
                .build();

        template = contractTemplateRepository.save(template);
        log.info("Template created with id: {}", template.getId());

        return template;
    }

    /**
     * Met à jour un template existant
     */
    @Transactional
    public ContractTemplate updateTemplate(UUID templateId, UpdateTemplateRequest request, User currentUser) {
        ContractTemplate template = contractTemplateRepository.findByIdWithCreatedBy(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template", "id", "unknown"));

        // Vérifier que c'est bien un template personnalisé (pas par défaut)
        if (Boolean.TRUE.equals(template.getIsDefault())) {
            throw new BadRequestException("Cannot modify default templates");
        }

        // Vérifier ownership
        if (!template.getCreatedBy().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You are not authorized to update this template");
        }

        log.info("Updating template: {} by user: {}", templateId, currentUser.getId());

        // Mettre à jour uniquement les champs fournis
        if (request.name() != null) {
            template.setName(request.name());
        }
        if (request.description() != null) {
            template.setDescription(request.description());
        }
        if (request.content() != null) {
            template.setContent(request.content());
        }
        if (request.isActive() != null) {
            template.setIsActive(request.isActive());
        }

        template = contractTemplateRepository.save(template);
        log.info("Template updated: {}", templateId);

        return template;
    }

    /**
     * Supprime un template (soft delete en désactivant)
     */
    @Transactional
    public void deleteTemplate(UUID templateId, User currentUser) {
        ContractTemplate template = contractTemplateRepository.findByIdWithCreatedBy(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template", "id", "unknown"));

        // Vérifier que c'est bien un template personnalisé (pas par défaut)
        if (Boolean.TRUE.equals(template.getIsDefault())) {
            throw new BadRequestException("Cannot delete default templates");
        }

        // Vérifier ownership
        if (!template.getCreatedBy().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You are not authorized to delete this template");
        }

        log.info("Deleting template: {} by user: {}", templateId, currentUser.getId());

        template.setIsActive(false);
        contractTemplateRepository.save(template);

        log.info("Template deleted (soft): {}", templateId);
    }

    /**
     * Récupère les templates accessibles par l'utilisateur (ses templates + templates par défaut)
     */
    public List<ContractTemplate> getMyTemplates(UUID userId, RentalType rentalType) {
        return contractTemplateRepository.findAccessibleTemplates(userId, rentalType);
    }

    /**
     * Duplique un template existant
     */
    @Transactional
    public ContractTemplate duplicateTemplate(UUID templateId, String newName, User currentUser) {
        ContractTemplate sourceTemplate = contractTemplateRepository.findByIdWithCreatedBy(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template", "id", "unknown"));

        log.info("Duplicating template: {} as '{}' by user: {}", templateId, newName, currentUser.getId());

        ContractTemplate duplicatedTemplate = ContractTemplate.builder()
                .name(newName)
                .description(sourceTemplate.getDescription())
                .rentalType(sourceTemplate.getRentalType())
                .content(sourceTemplate.getContent())
                .isDefault(false)
                .isActive(true)
                .createdBy(currentUser)
                .build();

        duplicatedTemplate = contractTemplateRepository.save(duplicatedTemplate);
        log.info("Template duplicated with id: {}", duplicatedTemplate.getId());

        return duplicatedTemplate;
    }
}
