package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.curriculum.model.Board;
import com.altafjava.school.domain.curriculum.repository.BoardRepository;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

	@Mock
	private BoardRepository boardRepository;

	private BoardService boardService;

	@BeforeEach
	void setUp() {
		boardService = new BoardService(boardRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void create_withNewCode_succeeds() {
		when(boardRepository.existsByCodeAndTenantId("CBSE", 1L)).thenReturn(false);
		when(boardRepository.save(any(Board.class))).thenAnswer(inv -> inv.getArgument(0));

		Board board = boardService.create("CBSE", "CBSE", null);

		assertEquals("CBSE", board.getName());
	}

	@Test
	void create_withDuplicateCode_throwsBusinessException() {
		when(boardRepository.existsByCodeAndTenantId("CBSE", 1L)).thenReturn(true);

		assertThrows(BusinessException.class, () -> boardService.create("CBSE", "CBSE", null));
	}
}
