import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CvssCalculatorComponent } from './cvss-calculator.component';

describe('CvssCalculatorComponent', () => {
  let component: CvssCalculatorComponent;
  let fixture: ComponentFixture<CvssCalculatorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CvssCalculatorComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(CvssCalculatorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have no result until every metric is selected', () => {
    expect(component.result()).toBeNull();

    component.selectMetric('AV', 'N');
    component.selectMetric('AC', 'L');
    component.selectMetric('PR', 'N');
    component.selectMetric('UI', 'N');
    component.selectMetric('S', 'U');
    component.selectMetric('C', 'H');
    component.selectMetric('I', 'H');

    expect(component.result()).toBeNull();

    component.selectMetric('A', 'H');

    expect(component.result()).toEqual({ score: 9.8, severity: 'Critical' });
  });

  it('should build the vector string as metrics are selected', () => {
    component.selectMetric('AV', 'N');
    component.selectMetric('AC', 'L');

    expect(component.currentVectorString()).toBe('CVSS:3.1/AV:N/AC:L');
  });

  it('should parse a pasted vector string and populate the selection', () => {
    component.vectorInput = 'CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H';
    component.applyVectorInput();

    expect(component.selection()).toEqual({ AV: 'N', AC: 'L', PR: 'N', UI: 'N', S: 'U', C: 'H', I: 'H', A: 'H' });
    expect(component.result()).toEqual({ score: 9.8, severity: 'Critical' });
    expect(component.vectorError()).toBeNull();
  });

  it('should set an error when the pasted vector string is incomplete', () => {
    component.vectorInput = 'CVSS:3.1/AV:N/AC:L';
    component.applyVectorInput();

    expect(component.vectorError()).not.toBeNull();
    expect(component.selection()).toEqual({});
  });

  it('should emit scoreSelected with the calculated score when useScore is called', () => {
    const emitSpy = spyOn(component.scoreSelected, 'emit');

    component.vectorInput = 'CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H';
    component.applyVectorInput();
    component.useScore();

    expect(emitSpy).toHaveBeenCalledWith(9.8);
  });

  it('should not emit scoreSelected when the vector is incomplete', () => {
    const emitSpy = spyOn(component.scoreSelected, 'emit');

    component.useScore();

    expect(emitSpy).not.toHaveBeenCalled();
  });

  it('should emit closed when close is called', () => {
    const emitSpy = spyOn(component.closed, 'emit');

    component.close();

    expect(emitSpy).toHaveBeenCalledWith();
  });

  it('should prefill the selection and vector input from a valid referenceVector on init', () => {
    const prefilledFixture = TestBed.createComponent(CvssCalculatorComponent);
    prefilledFixture.componentInstance.referenceVector = 'CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H';

    prefilledFixture.detectChanges();

    expect(prefilledFixture.componentInstance.selection()).toEqual({
      AV: 'N',
      AC: 'L',
      PR: 'N',
      UI: 'N',
      S: 'U',
      C: 'H',
      I: 'H',
      A: 'H',
    });

    expect(prefilledFixture.componentInstance.vectorInput).toBe('CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H');
    expect(prefilledFixture.componentInstance.result()).toEqual({ score: 9.8, severity: 'Critical' });
  });

  it('should ignore an invalid or incomplete referenceVector on init', () => {
    const prefilledFixture = TestBed.createComponent(CvssCalculatorComponent);
    prefilledFixture.componentInstance.referenceVector = 'not-a-vector';

    prefilledFixture.detectChanges();

    expect(prefilledFixture.componentInstance.selection()).toEqual({});
    expect(prefilledFixture.componentInstance.vectorInput).toBe('');
  });
});
