package com.company.keyvault.service;

import com.company.keyvault.dto.request.KeyUpdateRequest;
import com.company.keyvault.model.LicenseKey;
import com.company.keyvault.model.Product;
import com.company.keyvault.model.enums.*;
import com.company.keyvault.repository.*;
import org.junit.jupiter.api.*;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LicenseKeyServiceTest {
    private LicenseKeyRepository keys; private ProductRepository products; private LicenseKeyService service;

    @BeforeEach void setUp(){keys=mock(LicenseKeyRepository.class);products=mock(ProductRepository.class);service=new LicenseKeyService(keys,products,mock(KeyGeneratorService.class));}

    @Test void cannotReduceMaximumBelowActiveDeviceCount(){
        LicenseKey key=LicenseKey.builder().id("key-1").productId("product-1").currentActivations(2).maxActivations(3).build();
        when(keys.findById("key-1")).thenReturn(Optional.of(key));
        KeyUpdateRequest request=request(1,KeyType.TIME_LIMITED,Instant.now().plusSeconds(3600));
        assertThrows(IllegalArgumentException.class,()->service.updateKey("key-1",request));
        verify(keys,never()).save(any());
    }

    @Test void extendingExpiredLicenseReactivatesIt(){
        LicenseKey key=LicenseKey.builder().id("key-1").productId("product-1").status(KeyStatus.EXPIRED).currentActivations(0).maxActivations(1).validUntil(Instant.now().minusSeconds(60)).build();
        when(keys.findById("key-1")).thenReturn(Optional.of(key));when(keys.save(any())).thenAnswer(invocation->invocation.getArgument(0));when(products.findById("product-1")).thenReturn(Optional.of(Product.builder().name("Game").build()));
        service.updateKey("key-1",request(2,KeyType.TIME_LIMITED,Instant.now().plusSeconds(3600)));
        assertEquals(KeyStatus.ACTIVE,key.getStatus());assertEquals(2,key.getMaxActivations());
    }

    private KeyUpdateRequest request(int max,KeyType type,Instant until){KeyUpdateRequest request=new KeyUpdateRequest();request.setType(type);request.setMaxActivations(max);request.setValidFrom(Instant.now().minusSeconds(60));request.setValidUntil(until);return request;}
}
