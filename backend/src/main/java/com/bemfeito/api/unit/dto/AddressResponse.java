package com.bemfeito.api.unit.dto;



public record AddressResponse(Long cityId, String city, String state, String postalCode, String street, String number, String complement, String neighborhood) {  }
