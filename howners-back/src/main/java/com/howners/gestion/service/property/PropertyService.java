package com.howners.gestion.service.property;

import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.AddressDTO;
import com.howners.gestion.dto.request.CreatePropertyRequest;
import com.howners.gestion.dto.request.UpdatePropertyRequest;
import com.howners.gestion.dto.response.PropertyResponse;
import com.howners.gestion.exception.BusinessException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.rental.RentalStatus;
import com.howners.gestion.repository.PropertyRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final RentalRepository rentalRepository;
    private final UserRepository userRepository;
    private final com.howners.gestion.service.subscription.FeatureGateService featureGateService;

    @Transactional(readOnly = true)
    public List<PropertyResponse> findAllByCurrentUser() {
        UUID currentUserId = AuthService.getCurrentUserId();
        log.debug("Finding all properties for user {}", currentUserId);

        List<Property> properties = propertyRepository.findByOwnerId(currentUserId);
        return properties.stream()
                .map(PropertyResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PropertyResponse findById(UUID propertyId) {
        Property property = findPropertyByIdAndCheckOwnership(propertyId);
        return PropertyResponse.from(property);
    }

    @Transactional
    public PropertyResponse create(CreatePropertyRequest request) {
        UUID currentUserId = AuthService.getCurrentUserId();
        featureGateService.assertCanCreate(currentUserId, "PROPERTIES");
        log.info("Creating property '{}' for user {}", request.name(), currentUserId);

        User owner = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));

        Property property = Property.builder()
                .owner(owner)
                .name(request.name())
                .propertyType(request.propertyType())
                .addressLine1(request.address().addressLine1())
                .addressLine2(request.address().addressLine2())
                .city(request.address().city())
                .postalCode(request.address().postalCode())
                .department(request.address().department())
                .country(request.address().country())
                .surfaceArea(request.surfaceArea())
                .bedrooms(request.bedrooms())
                .bathrooms(request.bathrooms())
                .description(request.description())
                .condoFees(request.condoFees())
                .propertyTax(request.propertyTax())
                .businessTax(request.businessTax())
                .homeInsurance(request.homeInsurance())
                .purchasePrice(request.purchasePrice())
                .dpeRating(request.dpeRating())
                .gesRating(request.gesRating())
                .constructionYear(request.constructionYear())
                .floorNumber(request.floorNumber())
                .totalFloors(request.totalFloors())
                .heatingType(request.heatingType())
                .hasParking(request.hasParking())
                .hasElevator(request.hasElevator())
                .isFurnished(request.isFurnished())
                .propertyCondition(request.propertyCondition())
                .build();

        property = propertyRepository.save(property);
        log.info("Property created with id {}", property.getId());

        return PropertyResponse.from(property);
    }

    @Transactional
    public PropertyResponse update(UUID propertyId, UpdatePropertyRequest request) {
        Property property = findPropertyByIdAndCheckOwnership(propertyId);
        log.info("Updating property {}", propertyId);

        // Update only provided fields
        if (request.name() != null) {
            property.setName(request.name());
        }
        if (request.propertyType() != null) {
            property.setPropertyType(request.propertyType());
        }
        if (request.address() != null) {
            AddressDTO address = request.address();
            if (address.addressLine1() != null) property.setAddressLine1(address.addressLine1());
            if (address.addressLine2() != null) property.setAddressLine2(address.addressLine2());
            if (address.city() != null) property.setCity(address.city());
            if (address.postalCode() != null) property.setPostalCode(address.postalCode());
            if (address.department() != null) property.setDepartment(address.department());
            if (address.country() != null) property.setCountry(address.country());
        }
        if (request.surfaceArea() != null) {
            property.setSurfaceArea(request.surfaceArea());
        }
        if (request.bedrooms() != null) {
            property.setBedrooms(request.bedrooms());
        }
        if (request.bathrooms() != null) {
            property.setBathrooms(request.bathrooms());
        }
        if (request.description() != null) {
            property.setDescription(request.description());
        }
        if (request.condoFees() != null) {
            property.setCondoFees(request.condoFees());
        }
        if (request.propertyTax() != null) {
            property.setPropertyTax(request.propertyTax());
        }
        if (request.businessTax() != null) {
            property.setBusinessTax(request.businessTax());
        }
        if (request.homeInsurance() != null) {
            property.setHomeInsurance(request.homeInsurance());
        }
        if (request.purchasePrice() != null) {
            property.setPurchasePrice(request.purchasePrice());
        }
        if (request.dpeRating() != null) {
            property.setDpeRating(request.dpeRating());
        }
        if (request.gesRating() != null) {
            property.setGesRating(request.gesRating());
        }
        if (request.constructionYear() != null) {
            property.setConstructionYear(request.constructionYear());
        }
        if (request.floorNumber() != null) {
            property.setFloorNumber(request.floorNumber());
        }
        if (request.totalFloors() != null) {
            property.setTotalFloors(request.totalFloors());
        }
        if (request.heatingType() != null) {
            property.setHeatingType(request.heatingType());
        }
        if (request.hasParking() != null) {
            property.setHasParking(request.hasParking());
        }
        if (request.hasElevator() != null) {
            property.setHasElevator(request.hasElevator());
        }
        if (request.isFurnished() != null) {
            property.setIsFurnished(request.isFurnished());
        }
        if (request.propertyCondition() != null) {
            property.setPropertyCondition(request.propertyCondition());
        }

        property = propertyRepository.save(property);
        log.info("Property {} updated successfully", propertyId);

        return PropertyResponse.from(property);
    }

    @Transactional
    public void delete(UUID propertyId) {
        Property property = findPropertyByIdAndCheckOwnership(propertyId);
        log.info("Deleting property {}", propertyId);

        List<Rental> activeRentals = rentalRepository.findByPropertyId(propertyId).stream()
                .filter(r -> r.getStatus() == RentalStatus.ACTIVE || r.getStatus() == RentalStatus.PENDING)
                .toList();

        if (!activeRentals.isEmpty()) {
            throw new BusinessException(
                String.format("Cannot delete property. %d active or pending rental(s) are associated with this property. Please terminate them first.", activeRentals.size())
            );
        }

        propertyRepository.delete(property);
        log.info("Property {} deleted successfully", propertyId);
    }

    private Property findPropertyByIdAndCheckOwnership(UUID propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property", "id", propertyId.toString()));

        UUID currentUserId = AuthService.getCurrentUserId();
        if (!property.getOwner().getId().equals(currentUserId)) {
            throw new BusinessException("You don't have permission to access this property");
        }

        return property;
    }
}
