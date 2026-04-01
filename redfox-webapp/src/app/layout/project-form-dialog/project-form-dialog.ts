import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { Project } from '../../core/project/project.model';
import { ProjectService } from '../../core/project/project.service';

@Component({
  selector: 'app-project-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
  ],
  templateUrl: './project-form-dialog.html',
  styleUrl: './project-form-dialog.scss',
})
export class ProjectFormDialog implements OnInit {
  protected form!: FormGroup;
  protected isEdit = false;

  constructor(
    private readonly fb: FormBuilder,
    private readonly projectService: ProjectService,
    private readonly dialogRef: MatDialogRef<ProjectFormDialog>,
    @Inject(MAT_DIALOG_DATA) public data: Project | null,
  ) {}

  ngOnInit(): void {
    this.isEdit = !!this.data;
    this.form = this.fb.group({
      code: [{ value: this.data?.code ?? '', disabled: this.isEdit }, Validators.required],
      name: [this.data?.name ?? '', Validators.required],
      description: [this.data?.description ?? '', Validators.required],
    });
  }

  protected onSubmit(): void {
    if (!this.form.valid) {
      return;
    }

    if (this.isEdit && this.data) {
      this.projectService
        .updateProject(this.data.id, {
          name: this.form.value.name,
          description: this.form.value.description,
          version: this.data.version!,
        })
        .subscribe(() => this.dialogRef.close(true));
    } else {
      this.projectService
        .createProject(this.form.getRawValue())
        .subscribe(() => this.dialogRef.close(true));
    }
  }
}
