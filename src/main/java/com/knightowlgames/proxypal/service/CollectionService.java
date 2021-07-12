package com.knightowlgames.proxypal.service;

import com.knightowlgames.proxypal.datatype.MagicCard;
import com.knightowlgames.proxypal.repository.CollectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CollectionService {

	@Autowired
	private CollectionRepository collectionRepository;

	public MagicCard saveCard(MagicCard magicCard)
			{
				return null;
			}
}
