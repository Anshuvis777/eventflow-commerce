package com.eventflow.orderservice.mapper;

import com.eventflow.orderservice.dto.OrderResponse;
import com.eventflow.orderservice.entity.OrderEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    OrderResponse toResponse(OrderEntity entity);
}
