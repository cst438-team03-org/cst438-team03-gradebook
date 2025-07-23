package com.cst438.domain;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import java.util.List;

public interface AssignmentRepository extends CrudRepository<Assignment, Integer> {

    @Query("select a from Assignment a join a.section.term t join a.section.enrollments e where e.student.email=:email and t.year=:year and t.semester=:semester order by a.dueDate")
    List<Assignment> findByStudentEmailAndYearAndSemester(String email, int year, String semester);

    // Method to find assignments by sectionNo
    @Query("select a from Assignment a where a.section.sectionNo = :sectionNo order by a.dueDate")
    List<Assignment> findBySectionNo(int sectionNo);

    // Method to delete assignments by id
    @Modifying
    @Query("delete from Assignment a where a.id = :id")
    void deleteById(int id);
}
