package com.knightowlgames.proxypal.repository;

import com.knightowlgames.proxypal.datatype.MagicCard;
import org.springframework.data.repository.CrudRepository;

public interface CollectionRepository extends CrudRepository<MagicCard, Long> {
    MagicCard findByName(String cardName);
    MagicCard findBySetNameAndSetId(String setName, Integer setId);
}
