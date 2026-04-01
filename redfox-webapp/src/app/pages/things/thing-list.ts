import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Thing } from '../../core/thing/thing.model';
import { Project } from '../../core/project/project.model';
import { ThingService } from '../../core/thing/thing.service';
import { ProjectService } from '../../core/project/project.service';
import { ThingFormDialog } from '../../layout/thing-form-dialog/thing-form-dialog';
import { ConfirmDialog } from '../../layout/confirm-dialog/confirm-dialog';
import { BreadcrumbComponent } from '../../layout/breadcrumb/breadcrumb';

@Component({
  selector: 'app-thing-list',
  imports: [
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatPaginatorModule,
    MatDialogModule,
    BreadcrumbComponent,
  ],
  templateUrl: './thing-list.html',
  styleUrl: './thing-list.scss',
})
export class ThingList implements OnInit {
  protected readonly displayedColumns = ['code', 'name', 'description', 'actions'];
  protected readonly things = signal<Thing[]>([]);
  protected readonly project = signal<Project | null>(null);
  protected readonly totalElements = signal(0);
  protected readonly pageSize = signal(20);
  protected readonly pageIndex = signal(0);
  protected projectId = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly thingService: ThingService,
    private readonly projectService: ProjectService,
    private readonly dialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.projectId = this.route.snapshot.paramMap.get('projectId')!;
    this.projectService.getProject(this.projectId).subscribe((p) => this.project.set(p));
    this.loadThings();
  }

  protected onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadThings();
  }

  protected openCreateDialog(): void {
    const dialogRef = this.dialog.open(ThingFormDialog, {
      width: '480px',
      data: { projectId: this.projectId },
    });
    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.loadThings();
      }
    });
  }

  protected openEditDialog(thing: Thing): void {
    const dialogRef = this.dialog.open(ThingFormDialog, {
      width: '480px',
      data: { projectId: this.projectId, thing },
    });
    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.loadThings();
      }
    });
  }

  protected deleteThing(thing: Thing): void {
    const dialogRef = this.dialog.open(ConfirmDialog, {
      data: { title: 'Delete Thing', message: `Delete thing "${thing.name}"?` },
    });
    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.thingService.deleteThing(this.projectId, thing.id).subscribe(() => this.loadThings());
      }
    });
  }

  private loadThings(): void {
    this.thingService
      .getThings(this.projectId, this.pageIndex(), this.pageSize())
      .subscribe((result) => {
        this.things.set(result.content);
        this.totalElements.set(result.totalElements);
      });
  }
}
