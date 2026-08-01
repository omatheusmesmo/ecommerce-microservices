package com.ecommerce.service;

import com.ecommerce.entity.Category;
import com.ecommerce.repository.CategoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CategoryService {

    private static final Logger LOG = Logger.getLogger(CategoryService.class);

    @Inject
    CategoryRepository categoryRepository;

    public List<Category> findRoots() {
        return categoryRepository.findRoots();
    }

    public Category findById(String id) {
        return categoryRepository.findById(new ObjectId(id));
    }

    public List<Category> findChildren(String id) {
        return categoryRepository.findByParentId(id);
    }

    public List<Category> findBreadcrumb(String id) {
        Category category = findById(id);
        if (category == null) {
            throw new NoSuchElementException("Category " + id + " was not found");
        }

        LinkedList<Category> breadcrumb = new LinkedList<>();
        Category current = category;
        while (current != null) {
            breadcrumb.addFirst(current);
            current = current.parentId != null ? findById(current.parentId) : null;
        }
        return breadcrumb;
    }

    public Category create(Category category) {
        if (category.parentId != null && findById(category.parentId) == null) {
            throw new NoSuchElementException("Parent category " + category.parentId + " was not found");
        }

        categoryRepository.persist(category);
        LOG.infof("Category created successfully with ID: %s", category.id);
        return category;
    }

    public Category update(String id, Category updatedCategory) {
        Category existing = findById(id);
        if (existing == null) {
            return null;
        }

        if (updatedCategory.parentId != null) {
            if (findById(updatedCategory.parentId) == null) {
                throw new NoSuchElementException("Parent category " + updatedCategory.parentId + " was not found");
            }
            if (wouldCreateCycle(id, updatedCategory.parentId)) {
                throw new IllegalArgumentException("Category cannot become a descendant of itself");
            }
        }

        existing.name = updatedCategory.name;
        existing.parentId = updatedCategory.parentId;
        categoryRepository.update(existing);
        LOG.infof("Category updated: id=%s", id);
        return existing;
    }

    public boolean delete(String id) {
        if (!categoryRepository.findByParentId(id).isEmpty()) {
            throw new IllegalStateException("Cannot delete a category that still has child categories");
        }
        return categoryRepository.deleteById(new ObjectId(id));
    }

    private boolean wouldCreateCycle(String categoryId, String newParentId) {
        String current = newParentId;
        while (current != null) {
            if (current.equals(categoryId)) {
                return true;
            }
            Category parent = findById(current);
            current = parent != null ? parent.parentId : null;
        }
        return false;
    }
}
