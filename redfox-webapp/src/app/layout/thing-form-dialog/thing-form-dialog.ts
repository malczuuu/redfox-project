import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { Thing } from '../../core/thing/thing.model';
import { ThingService } from '../../core/thing/thing.service';

export interface ThingFormDialogData {
  projectId: string;
  thing?: Thing;
}

@Component({
  selector: 'app-thing-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
  ],
  templateUrl: './thing-form-dialog.html',
  styleUrl: './thing-form-dialog.scss',
})
export class ThingFormDialog implements OnInit {
  protected form!: FormGroup;
  protected isEdit = false;

  constructor(
    private readonly fb: FormBuilder,
    private readonly thingService: ThingService,
    private readonly dialogRef: MatDialogRef<ThingFormDialog>,
    @Inject(MAT_DIALOG_DATA) public data: ThingFormDialogData,
  ) {}

  ngOnInit(): void {
    this.isEdit = !!this.data.thing;
    this.form = this.fb.group({
      code: [{ value: this.data.thing?.code ?? '', disabled: this.isEdit }, Validators.required],
      name: [this.data.thing?.name ?? '', Validators.required],
      description: [this.data.thing?.description ?? '', Validators.required],
    });
  }

  protected onSubmit(): void {
    if (!this.form.valid) {
      return;
    }

    if (this.isEdit && this.data.thing) {
      this.thingService
        .updateThing(this.data.projectId, this.data.thing.id, {
          name: this.form.value.name,
          description: this.form.value.description,
          version: this.data.thing.version!,
        })
        .subscribe(() => this.dialogRef.close(true));
    } else {
      this.thingService
        .createThing(this.data.projectId, this.form.getRawValue())
        .subscribe(() => this.dialogRef.close(true));
    }
  }
}
