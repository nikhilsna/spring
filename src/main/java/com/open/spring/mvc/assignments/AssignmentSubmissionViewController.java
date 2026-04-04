package com.open.spring.mvc.assignments;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.open.spring.mvc.groups.Submitter;
import com.open.spring.mvc.groups.Groups;
import com.open.spring.mvc.person.Person;
import com.open.spring.mvc.person.PersonJpaRepository;

@RestController
@RequestMapping("/api/assignment-submission-view")
public class AssignmentSubmissionViewController {

    @Autowired
    private AssignmentSubmissionJPA submissionRepo;

    @Autowired
    private PersonJpaRepository personRepo;

    /**
     * Get all submissions for current user (or all if admin)
     * 
     * @return List of submissions filtered by user role
     */
    @GetMapping("/list")
    public ResponseEntity<?> getSubmissions() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Not authenticated"));
            }

            // Check if user is admin
            boolean isAdmin = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_TEACHER"));

            List<AssignmentSubmission> submissions;

            if (isAdmin) {
                // Admin sees all submissions
                submissions = submissionRepo.findAll();
            } else {
                // Regular user sees only their own submissions
                String username = auth.getName();
                Person person = personRepo.findByUid(username);
                if (person == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new ErrorResponse("User not found"));
                }
                // Get submissions where submitter is the current user
                submissions = submissionRepo.findBySubmitterId(person.getId());
            }

            // Convert to DTO and return
            List<SubmissionListDTO> dtos = submissions.stream()
                    .map(SubmissionListDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error fetching submissions: " + e.getMessage()));
        }
    }

    /**
     * DTO for submission list display
     */
    public static class SubmissionListDTO {
        public Long id;
        public Long assignmentId;
        public String assignmentName;
        public String submitterName;
        public Long submitterId;
        public String content;
        public String comment;
        public Double grade;
        public String feedback;
        public Boolean isLate;
        public Boolean isGroup;

        public SubmissionListDTO(AssignmentSubmission submission) {
            this.id = submission.getId();
            this.assignmentId = submission.getAssignment().getId();
            this.assignmentName = submission.getAssignment().getName();
            
            // Handle both Person and Groups submitters
            if (submission.getSubmitter() != null) {
                Submitter submitter = submission.getSubmitter();
                if (submitter instanceof Person) {
                    this.submitterName = ((Person) submitter).getName();
                    this.isGroup = false;
                } else if (submitter instanceof Groups) {
                    this.submitterName = ((Groups) submitter).getName();
                    this.isGroup = true;
                } else {
                    this.submitterName = "Unknown";
                    this.isGroup = false;
                }
                this.submitterId = submitter.getId();
            } else {
                this.submitterName = "Unknown";
                this.submitterId = null;
                this.isGroup = false;
            }
            
            this.content = submission.getContent();
            this.comment = submission.getComment();
            this.grade = submission.getGrade();
            this.feedback = submission.getFeedback();
            this.isLate = submission.getIsLate() != null ? submission.getIsLate() : false;
        }
    }

    /**
     * Simple error response
     */
    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}
