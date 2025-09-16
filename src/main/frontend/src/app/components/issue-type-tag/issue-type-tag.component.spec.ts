import { ComponentFixture, TestBed } from '@angular/core/testing';
import { IssueTypeTagComponent } from './issue-type-tag.component';
import { ColorService } from '../../services/color.service';

class MockColorService {
  getTypeTextColor(color: string): string {
    if (color === 'yellow') {
      return 'black';
    }
    return 'white';
  }
}

describe('IssueTypeTagComponent', () => {
  let component: IssueTypeTagComponent;
  let fixture: ComponentFixture<IssueTypeTagComponent>;
  let nativeElement: HTMLElement;
  let colorService: ColorService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IssueTypeTagComponent],
      providers: [{ provide: ColorService, useClass: MockColorService }],
    }).compileComponents();

    fixture = TestBed.createComponent(IssueTypeTagComponent);
    component = fixture.componentInstance;
    nativeElement = fixture.nativeElement;
    colorService = TestBed.inject(ColorService);
  });

  it('should create', () => {
    component.issueType = { name: 'Test', color: 'blue' };
    fixture.detectChanges();

    expect(component).toBeTruthy();
  });

  it('should display the issue type name and background color', () => {
    component.issueType = { name: 'Bug', color: 'red' };
    fixture.detectChanges();

    const spanElement = nativeElement.querySelector('.issue-type-tag') as HTMLSpanElement;

    expect(spanElement.textContent?.trim()).toBe('Bug');
    expect(spanElement.style.backgroundColor).toBe('red');
  });

  it('should call ColorService to get the correct text color', () => {
    const colorSpy = spyOn(colorService, 'getTypeTextColor').and.callThrough();

    component.issueType = { name: 'Task', color: 'yellow' };
    fixture.detectChanges();

    const spanElement = nativeElement.querySelector('.issue-type-tag') as HTMLSpanElement;

    expect(colorSpy).toHaveBeenCalledWith('yellow');

    expect(spanElement.style.color).toBe('black');
  });
});
