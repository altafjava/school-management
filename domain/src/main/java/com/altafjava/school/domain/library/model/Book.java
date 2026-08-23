package com.altafjava.school.domain.library.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "books")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Book extends SoftDeletableEntity {

	@Column(name = "isbn", length = 20)
	private String isbn;

	@Column(name = "title", nullable = false, length = 250)
	private String title;

	@Column(name = "author", length = 150)
	private String author;

	@Column(name = "publisher", length = 150)
	private String publisher;

	@Column(name = "category", length = 100)
	private String category;

	@Column(name = "active", nullable = false)
	private boolean active;

	public static Book create(String isbn, String title, String author, String publisher, String category) {
		return Book.builder()
				.isbn(isbn)
				.title(title)
				.author(author)
				.publisher(publisher)
				.category(category)
				.active(true)
				.build();
	}

	public void deactivate() {
		this.active = false;
	}
}
