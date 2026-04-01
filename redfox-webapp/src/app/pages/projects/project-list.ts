import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Project } from '../../core/project/project.model';
import { ProjectService } from '../../core/project/project.service';
import { ProjectFormDialog } from '../../layout/project-form-dialog/project-form-dialog';
import { ConfirmDialog } from '../../layout/confirm-dialog/confirm-dialog';

@Component({
  selector: 'app-project-list',
  imports: [
    RouterLink,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatPaginatorModule,
    MatDialogModule,
  ],
  templateUrl: './project-list.html',
  styleUrl: './project-list.scss',
})
export class ProjectList implements OnInit {
  protected readonly displayedColumns = ['code', 'name', 'description', 'actions'];
  protected readonly projects = signal<Project[]>([]);
  protected readonly totalElements = signal(0);
  protected readonly pageSize = signal(20);
  protected readonly pageIndex = signal(0);

  constructor(
    private readonly projectService: ProjectService,
    private readonly dialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.loadProjects();
  }

  protected onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadProjects();
  }

  protected openCreateDialog(): void {
    const dialogRef = this.dialog.open(ProjectFormDialog, { width: '480px' });
    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.loadProjects();
      }
    });
  }

  protected openEditDialog(project: Project): void {
    const dialogRef = this.dialog.open(ProjectFormDialog, {
      width: '480px',
      data: project,
    });
    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.loadProjects();
      }
    });
  }

  protected deleteProject(project: Project): void {
    const dialogRef = this.dialog.open(ConfirmDialog, {
      data: { title: 'Delete Project', message: `Delete project "${project.name}"?` },
    });
    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.projectService.deleteProject(project.id).subscribe(() => this.loadProjects());
      }
    });
  }

  private loadProjects(): void {
    this.projectService.getProjects(this.pageIndex(), this.pageSize()).subscribe((result) => {
      this.projects.set(result.content);
      this.totalElements.set(result.totalElements);
    });
  }
}
