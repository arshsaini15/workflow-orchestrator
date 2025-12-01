package com.arsh.workflow.service;

import com.arsh.workflow.dto.CreateTaskRequest;
import com.arsh.workflow.dto.CreateWorkflowRequest;
import com.arsh.workflow.dto.TaskResponse;
import com.arsh.workflow.dto.WorkflowResponse;
import com.arsh.workflow.enums.TaskStatus;
import com.arsh.workflow.enums.WorkflowStatus;
import com.arsh.workflow.exception.IllegalWorkflowOperationException;
import com.arsh.workflow.exception.WorkflowNotFoundException;
import com.arsh.workflow.mapper.TaskMapper;
import com.arsh.workflow.mapper.WorkflowMapper;
import com.arsh.workflow.model.Task;
import com.arsh.workflow.model.Workflow;
import com.arsh.workflow.repository.WorkflowRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;


@Service
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowRepository workflowRepository;

    public WorkflowServiceImpl(WorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
    }

    @Override
    public WorkflowResponse createWorkflow(CreateWorkflowRequest req) {
        Workflow workflow = WorkflowMapper.toEntity(req);

        workflow.setStatus(WorkflowStatus.CREATED);
        workflow = workflowRepository.save(workflow);

        return WorkflowMapper.toResponse(workflow);
    }

    @Override
    public WorkflowResponse getWorkflow(Long workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException(
                        "Workflow with id " + workflowId + " not found!"
                ));

        return WorkflowMapper.toResponse(workflow);
    }

    @Override
    public WorkflowResponse deleteWorkflow(Long workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException(
                        "Unable to find workflow with id " + workflowId)
                );

        WorkflowResponse res = WorkflowMapper.toResponse(workflow);
        workflowRepository.delete(workflow);

        return res;
    }

    @Override
    public TaskResponse deleteTask(Long workflowId, Long taskId) {

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException(
                        "Workflow with id " + workflowId + " not found"
                ));

        Task task = workflow.getTasks().stream()
                .filter(t -> t.getId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Task with id " + taskId + " not found in this workflow"
                ));

        TaskResponse res = TaskMapper.toResponse(task);

        workflow.removeTask(task);
        workflowRepository.save(workflow);

        return res;
    }


    @Override
    public TaskResponse addTask(Long workflowId, CreateTaskRequest req) {

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException(
                        "Workflow with id " + workflowId + "not found.")
                );

        Task task = TaskMapper.toEntity(req);

        task.setStatus(TaskStatus.TODO);
        task.setAssignedTo(null);
        workflow.addTask(task);

        workflowRepository.save(workflow);

        return TaskMapper.toResponse(task);
    }

    @Override
    public WorkflowResponse startWorkflow(Long workflowId) {

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException(
                        "Workflow with id " + workflowId + " not found."
                ));

        if (workflow.getTasks().isEmpty()) {
            throw new IllegalWorkflowOperationException(
                    "Workflow with id " + workflowId + " has no tasks!"
            );
        }

        workflow.setStatus(WorkflowStatus.ACTIVE);
        workflow = workflowRepository.save(workflow);

        return WorkflowMapper.toResponse(workflow);
    }

    @Override
    public WorkflowResponse completeWorkflow(Long workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException(
                        "Workflow with id " + workflowId + " not found."
                ));

        for (Task task : workflow.getTasks()) {
            if (task.getStatus() != TaskStatus.DONE) {
                throw new IllegalWorkflowOperationException(
                        "Cannot complete workflow. Task with id " + task.getId()
                                + " is not DONE. Status: " + task.getStatus()
                );
            }
        }

        workflow.setStatus(WorkflowStatus.COMPLETED);
        workflow = workflowRepository.save(workflow);

        return WorkflowMapper.toResponse(workflow);
    }

    @Override
    public Page<WorkflowResponse> getAllWorkflows(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Workflow> workflows = workflowRepository.findAll(pageable);

        return workflows.map(WorkflowMapper::toResponse);
    }
}
